package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.codeInsight.daemon.impl.analysis.XmlChangeAttributeValueIntentionFix
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.highlighting.BasicDomElementsInspection
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomHighlightingHelper
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
}
