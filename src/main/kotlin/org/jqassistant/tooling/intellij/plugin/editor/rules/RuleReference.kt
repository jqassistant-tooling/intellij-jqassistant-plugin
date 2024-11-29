package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.containers.map2Array
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingService
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

class RuleReference(
    element: PsiElement,
    private val name: String,
) : PsiReferenceBase<PsiElement?>(element) {
    override fun resolve(): PsiElement? {
        val definition = element.project.service<JqaRuleIndexingService>().resolve(name)
        return definition?.computeSource()
    }

    override fun getVariants(): Array<Any> =
        JqaRuleType.entries
            .flatMap { element.project.service<JqaRuleIndexingService>().getAll(it) }
            .map2Array { it.name }
}
