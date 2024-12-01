package org.jqassistant.tooling.intellij.plugin.editor.rules.actions.delete

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo

class RuleSafeDeleteProcessorDelegate : SafeDeleteProcessorDelegate {
    override fun findUsages(
        p0: PsiElement,
        p1: Array<out PsiElement>,
        p2: MutableList<in UsageInfo>,
    ): NonCodeUsageSearchInfo? {
        TODO("Not yet implemented")
        // search the whole project for usages of the rule
    }

    override fun getElementsToSearch(
        p0: PsiElement,
        p1: MutableCollection<out PsiElement>,
    ): MutableCollection<out PsiElement>? {
        TODO("Not yet implemented")
    }

    override fun getAdditionalElementsToDelete(
        p0: PsiElement,
        p1: MutableCollection<out PsiElement>,
        p2: Boolean,
    ): MutableCollection<PsiElement>? {
        TODO("Not yet implemented")
    }

    override fun findConflicts(p0: PsiElement, p1: Array<out PsiElement>): MutableCollection<String>? {
        TODO("Not yet implemented")
    }

    override fun preprocessUsages(p0: Project, p1: Array<out UsageInfo>): Array<UsageInfo>? {
        TODO("Not yet implemented")
    }

    override fun prepareForDeletion(p0: PsiElement) {
        TODO("Not yet implemented")
    }

    override fun isToSearchInComments(p0: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }

    override fun setToSearchInComments(p0: PsiElement?, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isToSearchForTextOccurrences(p0: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }

    override fun setToSearchForTextOccurrences(p0: PsiElement?, p1: Boolean) {
        TODO("Not yet implemented")
    }

    override fun handlesElement(p0: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }
}
