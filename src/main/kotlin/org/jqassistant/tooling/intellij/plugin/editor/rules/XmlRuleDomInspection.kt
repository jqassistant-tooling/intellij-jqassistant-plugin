package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.codeInsight.daemon.impl.analysis.XmlChangeAttributeValueIntentionFix
import com.intellij.openapi.components.service
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomFileElement
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

                if ("*" in id || "?" in id) {
                    holder.createProblem(
                        element.id,
                        MessageBundle.message("id.contains.wildcard.characters"),
                        XmlChangeAttributeValueIntentionFix(id.replace("*", "").replace("?", "")),
                    )
                }
            }
        }
    }

    override fun checkFileElement(
        domFileElement: DomFileElement<JqassistantRules>,
        holder: DomElementAnnotationHolder,
    ) {
        super.checkFileElement(domFileElement, holder)

        // Don't inspect anything if no valid config exists.
        domFileElement.file.project
            .service<JqaConfigurationService>()
            .getConfiguration() ?: return

        if (domFileElement.file.virtualFile in
            domFileElement.file.project
                .service<JqaConfigurationService>()
                .getAvailableRuleSources()
        ) {
            return
        }

        holder.createProblem(domFileElement, MessageBundle.message("inactive.rule.source.warning"))
    }
}
