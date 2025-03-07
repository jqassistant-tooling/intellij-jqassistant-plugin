package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class RuleRefactoringSupportProvider : RefactoringSupportProvider() {
    /**
     * Checks if element is eligible for safe delete refactoring.
     * Currently only works when placing caret in front of the <tag> element.
     * Placing it on the <tag> element gives us an element from the .xsd schema file.
     * Placing it on the id="value" gives us the right element, but IntelliJ does not recognize it as able to be refactored.
     */
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
        val manager = DomManager.getDomManager(element.project)

        val parentTag = element.parent.parent.parent // attribute -> value -> whitespace -> tag
        val parentXmlTag = parentTag as? XmlTag?
        val domParentTag = manager.getDomElement(parentXmlTag)

        val tag = element as? XmlTag?
        val domElement = manager.getDomElement(tag)

        return domElement is RuleBase || domParentTag is RuleBase
    }
}
