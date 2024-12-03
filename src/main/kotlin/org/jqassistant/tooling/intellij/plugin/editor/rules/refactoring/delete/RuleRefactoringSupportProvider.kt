package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class RuleRefactoringSupportProvider : RefactoringSupportProvider() {
    /**
     * Checks if element is eligible for safe delete refactoring.
     * Currently only works on the id attribute of a rule
     */
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java)
        val manager = DomManager.getDomManager(element.project)
        val domElement = manager.getDomElement(xmlTag)
        println("available: $element, $xmlTag, $manager, $domElement, ${domElement is RuleBase}")
        return domElement is RuleBase
    }
}
