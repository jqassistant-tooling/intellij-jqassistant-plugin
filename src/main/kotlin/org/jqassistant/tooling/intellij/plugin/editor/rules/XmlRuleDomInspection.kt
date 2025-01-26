package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.codeInsight.daemon.impl.analysis.RemoveTagIntentionFix
import com.intellij.codeInsight.daemon.impl.analysis.XmlChangeAttributeValueIntentionFix
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.highlighting.BasicDomElementsInspection
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomHighlightingHelper
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.JqassistantRules
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class XmlRuleDomInspection : BasicDomElementsInspection<JqassistantRules>(JqassistantRules::class.java) {
    override fun checkDomElement(
        element: DomElement,
        holder: DomElementAnnotationHolder,
        helper: DomHighlightingHelper,
    ) {
        super.checkDomElement(element, holder, helper)
        if (element is RuleBase) {
            checkWildcard(element, holder, helper)
            checkUnusedRule(element, holder, helper)
        }
    }

    private fun checkWildcard(element: RuleBase, holder: DomElementAnnotationHolder, helper: DomHighlightingHelper) {
        val id = element.id.stringValue ?: return

        if ("*" in id || "?" in id) {
            holder.createProblem(
                element.id,
                MessageBundle.message("id.contains.wildcard.characters"),
                XmlChangeAttributeValueIntentionFix(id.replace("*", "").replace("?", "")),
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
                    element.id,
                    MessageBundle.message("annotator.inactive.rule"),
                    RemoveTagIntentionFix(xmlElement.name, xmlElement),
                ).highlightWholeElement()
        }
    }
}
