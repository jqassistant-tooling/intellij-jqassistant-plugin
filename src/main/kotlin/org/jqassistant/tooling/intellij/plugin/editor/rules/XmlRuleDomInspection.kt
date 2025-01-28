package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.codeInsight.daemon.impl.analysis.RemoveTagIntentionFix
import com.intellij.codeInsight.daemon.impl.analysis.XmlChangeAttributeValueIntentionFix
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomUtil
import com.intellij.util.xml.GenericDomValue
import com.intellij.util.xml.highlighting.BasicDomElementsInspection
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomHighlightingHelper
import org.jqassistant.tooling.intellij.plugin.common.ChangeXmlTagNameQuickFix
import org.jqassistant.tooling.intellij.plugin.common.WildcardUtil
import org.jqassistant.tooling.intellij.plugin.common.arrayOfNotNull
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.JqassistantRules
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import training.featuresSuggester.getParentOfType

class XmlRuleDomInspection : BasicDomElementsInspection<JqassistantRules>(JqassistantRules::class.java) {
    override fun checkDomElement(
        element: DomElement,
        holder: DomElementAnnotationHolder,
        helper: DomHighlightingHelper,
    ) {
        super.checkDomElement(element, holder, helper)
        when (element) {
            is RuleBase -> {
                checkWildcard(element, holder, helper)
                checkUnusedRule(element, holder, helper)
            }

            is GenericDomValue<*> -> {
                checkRuleReference(element, holder, helper)
            }
        }
    }

    private fun checkWildcard(element: RuleBase, holder: DomElementAnnotationHolder, helper: DomHighlightingHelper) {
        val id = element.id.stringValue ?: return

        if (WildcardUtil.looksLikeWildcard(id)) {
            holder.createProblem(
                element.id,
                HighlightSeverity.WARNING,
                MessageBundle.message("id.contains.wildcard.characters"),
                XmlChangeAttributeValueIntentionFix(WildcardUtil.stripWildcard(id)),
            )
        }
    }

    private fun checkUnusedRule(element: RuleBase, holder: DomElementAnnotationHolder, helper: DomHighlightingHelper) {
        val idValue = element.id.stringValue ?: return
        val configService = element.manager.project.service<JqaConfigurationService>()
        val ruleSet = configService.getEffectiveRules() ?: return
        val allRules = ruleSet.groups + ruleSet.concepts.keys + ruleSet.constraints.keys
        val ruleUsed = allRules.find { rule -> rule.id == idValue } != null

        val xmlElement = element.xmlElement as XmlTag

        if (!ruleUsed) {
            holder
                .createProblem(
                    element,
                    MessageBundle.message("annotator.inactive.rule"),
                    RemoveTagIntentionFix(xmlElement.name, xmlElement),
                ).highlightWholeElement()
        }
    }

    private fun checkRuleReference(
        element: GenericDomValue<*>,
        holder: DomElementAnnotationHolder,
        helper: DomHighlightingHelper,
    ) {
        val psi = DomUtil.getValueElement(element) ?: return
        for (reference in psi.references) {
            if (reference is RuleReference) {
                val results = reference.multiResolve(false)
                if (WildcardUtil.looksLikeWildcard(reference.name)) {
                    if (results.isEmpty()) {
                        holder.createProblem(
                            element,
                            HighlightSeverity.WARNING,
                            MessageBundle.message("wildcard.matches.nothing"),
                            *arrayOfNotNull(
                                psi.getParentOfType<XmlTag>()?.let { RemoveTagIntentionFix(it.name, it) },
                            ),
                        )
                    }
                } else {
                    val types = mutableSetOf<JqaRuleType>()
                    for (result in results) {
                        if (result.isValidResult) {
                            return
                        }
                        result.type?.let { types.add(it) }
                    }

                    holder.createProblem(
                        element,
                        ProblemHighlightType.ERROR,
                        MessageBundle.message("cannot.resolve", element.stringValue ?: ""),
                        null,
                        *makeTagChangeQuickFixes(psi.getParentOfType<XmlTag>(), types),
                    )
                }
            }
        }
    }

    // Resolve problems are checked manually in [checkDomElement].
    override fun shouldCheckResolveProblems(value: GenericDomValue<*>?): Boolean = false

    /**
     * Creates quick fixes to adjust the xml tag to one that supports an available type.
     *
     * E.g. for an <includeConstraint> tag that resolved to a group it would suggest changing to an <includeGroup> tag.
     */
    private fun makeTagChangeQuickFixes(xmlTag: XmlTag?, validTypes: Set<JqaRuleType>): Array<LocalQuickFix> {
        if (xmlTag == null) return emptyArray()

        val includeMap =
            mapOf(
                JqaRuleType.GROUP to "includeGroup",
                JqaRuleType.CONSTRAINT to "includeConstraint",
                JqaRuleType.CONCEPT to "includeConcept",
            )

        // Only replace xml tags that are semantically equivalent.
        if (xmlTag.name in includeMap.values) {
            return validTypes.map { type -> ChangeXmlTagNameQuickFix(includeMap[type]!!, xmlTag) }.toTypedArray()
        }

        return emptyArray()
    }
}
