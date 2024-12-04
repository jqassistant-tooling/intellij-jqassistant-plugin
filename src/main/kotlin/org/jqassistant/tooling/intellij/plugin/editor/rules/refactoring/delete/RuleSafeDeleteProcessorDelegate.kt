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
        p0: PsiElement,
        p1: Array<out PsiElement>,
        p2: MutableList<in UsageInfo>,
    ): NonCodeUsageSearchInfo? {
        val xmlTag = PsiTreeUtil.getParentOfType(p0, com.intellij.psi.xml.XmlTag::class.java)
        val manager = DomManager.getDomManager(p0.project)
        val domElement = manager.getDomElement(xmlTag) as? RuleBase ?: return null
        println("called findUsages")
        // Search for all usages of the element with the same ID
        val project = p0.project
        val id = domElement.id.value
        SafeDeleteProcessor.findGenericElementUsages(
            p0,
            p2,
            p1,
            GlobalSearchScope.projectScope(p0.getProject()),
        )

        return NonCodeUsageSearchInfo(SafeDeleteProcessor.getDefaultInsideDeletedCondition(p1), p0)
    }

    override fun getElementsToSearch(
        p0: PsiElement,
        p1: MutableCollection<out PsiElement>,
    ): MutableCollection<out PsiElement>? = p1

    override fun getAdditionalElementsToDelete(
        p0: PsiElement,
        p1: MutableCollection<out PsiElement>,
        p2: Boolean,
    ): MutableCollection<PsiElement>? = null

    override fun findConflicts(
        p0: PsiElement,
        p1: Array<out PsiElement>,
    ): MutableCollection<String>? = null

    override fun preprocessUsages(
        p0: Project,
        p1: Array<out UsageInfo>,
    ): Array<UsageInfo> = p1.map2Array { it }

    override fun prepareForDeletion(p0: PsiElement) {
    }

    override fun isToSearchInComments(p0: PsiElement?): Boolean = false

    override fun setToSearchInComments(
        p0: PsiElement?,
        p1: Boolean,
    ) {
        // do nothing
    }

    override fun isToSearchForTextOccurrences(p0: PsiElement?): Boolean = false

    override fun setToSearchForTextOccurrences(
        p0: PsiElement?,
        p1: Boolean,
    ) {
        // do nothing
    }

    override fun handlesElement(p0: PsiElement?): Boolean {
        // DOM - Modell
        // get the xml tag from the psi element
        // dom element
        // name
        // indexing service with name
        // is there a rule with this name?
        val xmlTag = p0 as XmlTag
        val manager = DomManager.getDomManager(p0.project)
        val domElement = manager.getDomElement(xmlTag)
        println("handle: $p0, $xmlTag, $manager, $domElement, ${domElement is RuleBase}")
        return domElement is RuleBase
    }
}
