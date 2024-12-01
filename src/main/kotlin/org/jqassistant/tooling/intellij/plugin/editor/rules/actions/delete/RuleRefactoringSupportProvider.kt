package org.jqassistant.tooling.intellij.plugin.editor.rules.actions.delete

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class RuleRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isSafeDeleteAvailable(element: PsiElement): Boolean = super.isSafeDeleteAvailable(element)
}
