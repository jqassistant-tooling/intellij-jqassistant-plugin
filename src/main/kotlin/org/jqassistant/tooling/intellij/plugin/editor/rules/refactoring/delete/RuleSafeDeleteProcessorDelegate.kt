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
    /**
     * Find usages of the `element` and fill `result` with them.
     * Is called during `BaseRefactoringProcessor.findUsages()` under modal progress in read action.
     *
     * @param element              an element selected for deletion.
     * @param allElementsToDelete  all elements selected for deletion.
     * @param result               list of `UsageInfo` to store found usages
     * @return `null` if element should not be searched in text occurrences/comments though corresponding settings were enabled, otherwise
     *                             bean with the information how to detect if an element is inside all elements to delete (e.g. `SafeDeleteProcessor.getDefaultInsideDeletedCondition(PsiElement[])`)
     *                             and current element.
     */
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

    /**
     * Returns elements that are searched for usages of the element selected for deletion. Called before the refactoring dialog is shown.
     * May show UI to ask if additional elements should be deleted along with the specified selected element.
     *
     * @param element an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @return additional elements to search for usages, or null if the user has cancelled the refactoring.
     */
    override fun getElementsToSearch(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
    ): MutableCollection<out PsiElement>? {
        // Adding elements makes the refactoring not even delete the element it was called on
        val result: MutableCollection<PsiElement> = mutableListOf(element)
        return result
    }

    /**
     * Returns the list of additional elements to be deleted. Called after the refactoring dialog is shown.
     * May show UI to ask the user if some additional elements should be deleted along with the
     * specified selected element.
     * This implementation returns all `providesConcept` tags that reference the selected element.
     *  TODO - also requiresConcept tag?
     *
     * @param element an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @return additional elements to search for usages, or null if no additional elements were chosen.
     */
    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
        askUser: Boolean,
    ): MutableCollection<PsiElement>? {
        val result = findRefIdUsages(element, "providesConcept")
        val result2 = findRefIdUsages(element, "requiresConcept")
        return result.toMutableList().also { it.addAll(result2) }
    }

    /**
     * Detects usages which are not safe to delete.
     *
     * @param element an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @return collection of conflict messages which would be shown to the user before delete can be performed.
     */
    override fun findConflicts(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
    ): MutableCollection<String>? {
        val conflicts = allElementsToDelete.filter { (it as? XmlTag?)?.name == "requiresConcept" }.map { it as XmlTag }
        val messages = mutableListOf<String>()
        conflicts.forEach {
            messages.add(
                "The concept <i>${
                    (element as XmlTag).getAttributeValue(
                        "id",
                    )
                }</i> <b>is required</b> by another concept via <i href='#'>${it.name}</i>",
            )
        }
        return messages
    }

    /**
     * Called after the user has confirmed the refactoring. Can filter out some of the usages
     * found by the refactoring. May show UI to ask the user if some of the usages should
     * be excluded.
     *
     * @param project the project where the refactoring happens.
     * @param usages all usages to be processed by the refactoring.
     * @return the filtered list of usages, or null if the user has cancelled the refactoring.
     */
    override fun preprocessUsages(
        project: Project,
        usages: Array<out UsageInfo>,
    ): Array<UsageInfo> = usages.map2Array { it }

    /**
     * Prepares an element for deletion e.g., normalizing declaration so the element declared in the same declaration won't be affected by deletion.
     *
     * Called during `BaseRefactoringProcessor.performRefactoring(UsageInfo[])` under write action
     *
     * @param element an element selected for deletion.
     */
    override fun prepareForDeletion(element: PsiElement) {
    }

    /**
     * Called to set initial value for "Search in comments" checkbox.
     * @return `true` if previous safe delete was executed with "Search in comments" option on.
     */
    override fun isToSearchInComments(element: PsiElement?): Boolean = false

    /**
     * Called to save chosen for given `element` "Search in comments" value.
     */
    override fun setToSearchInComments(
        element: PsiElement?,
        enabled: Boolean,
    ) {
        // do nothing
    }

    /**
     * Called to set initial value for "Search for text occurrence" checkbox.
     * @return `true` if previous safe delete was executed with "Search for test occurrences" option on.
     */
    override fun isToSearchForTextOccurrences(element: PsiElement?): Boolean = false

    /**
     * Called to save chosen for given `element` "Search for text occurrences" value.
     */
    override fun setToSearchForTextOccurrences(
        element: PsiElement?,
        enabled: Boolean,
    ) {
        // do nothing
    }

    /**
     * @return `true` if delegates can process `element`.
     */
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

    /**
     * Find refId usages in the project
     * @param element the element that is being deleted
     * @param referenceTag the tag that contains the refId attribute, e.g. providesConcept or requiresConcept
     */
    private fun findRefIdUsages(
        element: PsiElement,
        referenceTag: String,
    ): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val psiFile = element.containingFile
        val psiXmlTags: Array<out PsiElement> =
            PsiTreeUtil.collectElements(psiFile) { (it as? XmlTag?) != null }
        for (tag in psiXmlTags) {
            val xmlTag = tag as XmlTag
            val providesConceptTags = xmlTag.findSubTags(referenceTag)
            for (providesConceptTag in providesConceptTags) {
                if (providesConceptTag.getAttributeValue("refId") == (element as XmlTag).getAttributeValue("id")) {
                    result.add(providesConceptTag)
                }
            }
        }
        return result.toList()
    }
}
