package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlElementType
import com.intellij.psi.xml.XmlTag
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class RuleRefactoringSupportProvider : RefactoringSupportProvider() {
    /**
     * Checks if element is eligible for safe delete refactoring.
     * Currently only works on the id attribute of a rule
     */
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
        val tag = element as? XmlTag?
        val manager = DomManager.getDomManager(element.project)
        val domElement = manager.getDomElement(tag)

        /* Clicking the <tag> element will return the parent element of its schema.xsd file,
         * which gives us no information about the rule type or if it is one, as we are inside a completely different file now
         */
        if (element.context.elementType == XmlElementType.XML_TAG) {
            val elements: MutableList<XmlTag> = mutableListOf()
            element.children.forEach { elements.addIfNotNull(PsiTreeUtil.getParentOfType(it, XmlTag::class.java)) }
            elements.forEach { println(it) }
        }
        println("available: $element, $tag, $manager, $domElement, ${domElement is RuleBase}")
        return domElement is RuleBase
    }
}
