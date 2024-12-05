package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class RuleFileSafeDeleteProcessorDelegate : RuleSafeDeleteProcessorDelegate() {
    /**
     * Determines if this delegate can handle the given element.
     *
     * @param element the element to check.
     * @return `true` if this delegate can handle the element, `false` otherwise.
     */
    override fun handlesElement(element: PsiElement?): Boolean {
        if (element == null) return false
        val files = findEligiblePsiFiles(element)
        return files.contains(element)
    }

    /**
     * Returns the list of additional elements to be deleted. Called after the refactoring dialog is shown.
     *
     * @param element the element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @param askUser whether to ask the user for confirmation.
     * @return additional elements to delete, or `null` if no additional elements were chosen.
     */
    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
        askUser: Boolean,
    ): MutableCollection<PsiElement> {
        val result = mutableSetOf<PsiElement>()
        val domManager = DomManager.getDomManager(element.project)
        val files = findEligiblePsiFiles(element)
        for (file in files) {
            val psiRuleTags =
                PsiTreeUtil.collectElements(file) { (it as? XmlTag?) != null }
            for (rule in psiRuleTags) {
                val domElement = domManager.getDomElement(rule as XmlTag)
                if (domElement is RuleBase) {
                    result.addAll(super.getAdditionalElementsToDelete(rule, allElementsToDelete, askUser))
                }
            }
        }
        return result
    }
}
