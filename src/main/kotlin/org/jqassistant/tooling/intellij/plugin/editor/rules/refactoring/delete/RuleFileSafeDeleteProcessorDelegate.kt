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
     * In this case contains the PsiElements to references of each
     * constraint, group and concept in this file and also the constraints, groups and concepts themselves.
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
        val psiXmlTags =
            PsiTreeUtil.collectElements(element) { (it as? XmlTag?) != null }
        for (psiXmlTag in psiXmlTags) {
            val domElement = domManager.getDomElement(psiXmlTag as XmlTag)
            if (domElement is RuleBase) {
                result.add(psiXmlTag)
                result.addAll(super.getAdditionalElementsToDelete(psiXmlTag, allElementsToDelete, askUser))
            }
        }
        return result
    }

    /**
     * Find all conflicts with the deletion of the file
     * The conflicts for each group, concept and constraint in this file are calculated by the RuleSafeDeleteProcessorDelegate,
     * which is called by IntelliJ automatically, so we don't need to handle those.
     *
     * @param element the file that is being deleted
     * @param allElementsToDelete all elements selected for deletion.
     * @return the list of conflicts as String to be display to the user
     */
    override fun findConflicts(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
    ): MutableCollection<String> {
        return mutableListOf()
    }
}
