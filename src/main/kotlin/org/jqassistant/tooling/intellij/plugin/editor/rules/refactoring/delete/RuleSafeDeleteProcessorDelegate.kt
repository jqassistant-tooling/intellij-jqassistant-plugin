package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.map2Array
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.NameIndex
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

open class RuleSafeDeleteProcessorDelegate : SafeDeleteProcessorDelegate {
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
        // Search for all usages of the element with the same ID
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
    ): MutableCollection<out PsiElement> {
        // Adding elements makes the refactoring not even delete the element it was called on
        val result: MutableCollection<PsiElement> = mutableListOf(element)
        return result
    }

    /**
     * Returns the list of additional elements to be deleted. Called after the refactoring dialog is shown.
     * May show UI to ask the user if some additional elements should be deleted along with the
     * specified selected element.
     * This implementation returns all `providesConcept` tags that reference the selected element.
     *
     * @param element an element selected for deletion.
     * @param allElementsToDelete all elements selected for deletion.
     * @return additional elements to search for usages, or null if no additional elements were chosen.
     */
    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: MutableCollection<out PsiElement>,
        askUser: Boolean,
    ): MutableCollection<PsiElement> {
        val files = findEligiblePsiFiles(element)
        val result = mutableSetOf<PsiElement>()
        for (file in files) {
            result.addAll(findRefIdUsages(element, file, "providesConcept"))
            result.addAll(findRefIdUsages(element, file, "requiresConcept"))
            result.addAll(findRefIdUsages(element, file, "includeConcept"))
            result.addAll(findRefIdUsages(element, file, "includeConstraint"))
            result.addAll(findRefIdUsages(element, file, "includeGroup"))
        }
        return result
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
    ): MutableCollection<String> {
        /*
         * Explicitly check for conflicts in the context of the tag, its parent and the $element to be deleted
         * */
        val messages = mutableListOf<String>()
        val possibleConflicts =
            allElementsToDelete.filterIsInstance<XmlTag>().filter { it.name == "requiresConcept" }
        val noDeletableParents = possibleConflicts.filter { !allElementsToDelete.contains(it.parent) }
        val relatedConflicts =
            noDeletableParents.filter {
                (element as? XmlTag)?.getAttributeValue("id") == it.getAttributeValue("refId") &&
                    it.getAttributeValue("refId") != null
            }

        relatedConflicts.forEach {
            messages.add(
                "The ${(it.parent as XmlTag).name} <i>${
                    (it).getAttributeValue(
                        "refId",
                    )
                }</i> <b>is required</b> by concept <i>${
                    (it.parent as XmlTag).getAttributeValue(
                        "id",
                    )
                }</i> via <i>${it.name}</i> in file <i>${it.containingFile.name}</i>.",
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
    override fun preprocessUsages(project: Project, usages: Array<out UsageInfo>): Array<UsageInfo> =
        usages.map2Array { it }

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
    override fun setToSearchInComments(element: PsiElement?, enabled: Boolean) {
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
    override fun setToSearchForTextOccurrences(element: PsiElement?, enabled: Boolean) {
        // do nothing
    }

    /**
     * @return `true` if delegates can process `element`.
     */
    override fun handlesElement(element: PsiElement?): Boolean {
        val manager = DomManager.getDomManager(element?.project)

        val parentTag = element?.parent?.parent?.parent // attribute -> value -> whitespace -> tag
        val parentXmlTag = parentTag as? XmlTag?
        val domParentTag = manager.getDomElement(parentXmlTag)

        val tag = element as? XmlTag?
        val domElement = manager.getDomElement(tag)

        return domElement is RuleBase || domParentTag is RuleBase
    }

    /**
     * Find refId usages in the project
     * TODO this operation belongs into a the JqaRuleIndexingService
     * @param element the element that is being deleted
     * @param referenceTag the tag that contains the refId attribute, e.g. providesConcept or requiresConcept
     */
    private fun findRefIdUsages(element: PsiElement, psiFile: PsiElement, referenceTag: String): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
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

    /**
     * Find all eligible psiFiles in the project (jqa rule files)
     * TODO this operation belongs into a the JqaRuleIndexingService
     * @param element the element that is being deleted
     */
    protected fun findEligiblePsiFiles(element: PsiElement): List<PsiElement> {
        val fileIndex =
            FileBasedIndex
                .getInstance()

        val keys = fileIndex.getAllKeys(NameIndex.Util.NAME, element.project)
        val files = mutableSetOf<VirtualFile>()

        for (key in keys) {
            val keyFiles =
                fileIndex
                    .getContainingFilesIterator(
                        NameIndex.Util.NAME,
                        key,
                        GlobalSearchScope.projectScope(element.project),
                    )
            for (file in keyFiles) files.add(file)
        }
        return files.mapNotNull { it.findPsiFile(element.project)?.containingFile }
    }
}
