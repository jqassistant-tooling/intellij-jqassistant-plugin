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
        when (element) {
            is RuleBase -> {
                val id = element.id.stringValue ?: return

                // Check for reserved wildcard characters
                if ("*" in id || "?" in id) {
                    holder.createProblem(
                        element.id,
                        MessageBundle.message("id.contains.wildcard.characters"),
                        XmlChangeAttributeValueIntentionFix(id.replace("*", "").replace("?", "")),
                    )
                }

                // Check if this rule is used
                val configService = element.manager.project.service<JqaConfigurationService>()

                // Do not annotate if the effective rule set is currently unknown
                val ruleSet = configService.getEffectiveRules() ?: return

                val allRules = ruleSet.groups + ruleSet.concepts.keys + ruleSet.constraints.keys
                val ruleUsed = allRules.find { rule -> rule.id == id } != null

                val xmlElement = element.xmlElement as XmlTag

                if (!ruleUsed) {
                    holder.createProblem(
                        element.id,
                        MessageBundle.message("annotator.inactive.rule"),
                        RemoveTagIntentionFix(xmlElement.name, xmlElement),
                    )
                }
            }
        }
    }
}
