package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.map2Array
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class RuleSafeDeleteProcessorDelegate : SafeDeleteProcessorDelegate {
    override fun findUsages(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
        result: MutableList<in UsageInfo>,
    ): NonCodeUsageSearchInfo? {
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java)
        val manager = DomManager.getDomManager(element.project)
        val domElement = manager.getDomElement(xmlTag) as? RuleBase ?: return null
        println("called findUsages")
        // Search for all usages of the element with the same ID
        val project = element.project
        val id = domElement.id.value
        SafeDeleteProcessor.findGenericElementUsages(
            element,
            result,
            allElementsToDelete,
            GlobalSearchScope.projectScope(element.project),
        )

        return NonCodeUsageSearchInfo(
            SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete),
            element,
        )
    }

    override fun getElementsToSearch(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
    ): MutableCollection<out PsiElement>? {
        val result: MutableCollection<PsiElement> = mutableListOf(element)

        // val project = element.project
        result.add(element.parent)

        return result
    }

    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
        askUser: Boolean,
    ): MutableCollection<PsiElement>? = null

    override fun findConflicts(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
    ): MutableCollection<String>? = null

    override fun preprocessUsages(
        project: Project,
        usages: Array<out UsageInfo>,
    ): Array<UsageInfo> = usages.map2Array { it }

    override fun prepareForDeletion(element: PsiElement) {
    }

    override fun isToSearchInComments(element: PsiElement?): Boolean = false

    override fun setToSearchInComments(
        element: PsiElement?,
        enabled: Boolean,
    ) {
        // do nothing
    }

    override fun isToSearchForTextOccurrences(element: PsiElement?): Boolean = false

    override fun setToSearchForTextOccurrences(
        element: PsiElement?,
        enabled: Boolean,
    ) {
        // do nothing
    }

    override fun handlesElement(element: PsiElement?): Boolean {
        // DOM - Modell
        // get the xml tag from the psi element
        // dom element
        // name
        // indexing service with name
        // is there a rule with this name?
        val xmlTag = element as XmlTag
        val manager = DomManager.getDomManager(element.project)
        val domElement = manager.getDomElement(xmlTag)
        println("handle: $element, $xmlTag, $manager, $domElement, ${domElement is RuleBase}")
        return domElement is RuleBase
    }
}
