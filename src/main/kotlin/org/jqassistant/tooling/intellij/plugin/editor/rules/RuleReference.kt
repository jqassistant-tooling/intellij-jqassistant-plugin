package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.containers.map2Array
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingService
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

class RuleReference(
    element: PsiElement,
    private val name: String,
) : PsiReferenceBase<PsiElement?>(element),
    PsiPolyVariantReference {
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.size == 1) results[0].element else null
    }

    // FIXME: Remove @OptIn if IntelliJ 2023.1 support is dropped.
    @OptIn(ExperimentalStdlibApi::class)
    override fun getVariants(): Array<Any> =
        JqaRuleType.entries
            .flatMap { element.project.service<JqaRuleIndexingService>().getAll(it) }
            .map2Array { it.name }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val results = element.project.service<JqaRuleIndexingService>().resolve(name)
        return results
            .mapNotNull { definition ->
                definition.computeSource()?.let { PsiElementResolveResult(it, true) }
            }.toTypedArray()
    }
}
