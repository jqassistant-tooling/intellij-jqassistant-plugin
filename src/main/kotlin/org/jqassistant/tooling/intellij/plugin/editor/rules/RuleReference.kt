package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.containers.map2Array
import org.jqassistant.tooling.intellij.plugin.common.WildcardUtil
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingService
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

class RuleReference(
    element: PsiElement,
    val name: String,
    private val jqaRuleType: JqaRuleType? = null,
    private val soft: Boolean = false,
) : PsiPolyVariantReferenceBase<PsiElement?>(element),
    PsiPolyVariantReference {
    override fun getVariants(): Array<Any> =
        element.project
            .service<JqaRuleIndexingService>()
            .getAll(jqaRuleType)
            .map2Array { it.name }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val results =
            if (WildcardUtil.looksLikeWildcard(name)) {
                // For wildcards, we filter during resolution to only show the fitting type.
                element.project.service<JqaRuleIndexingService>().resolve(name, jqaRuleType)
            } else {
                // If this is not a wildcard we also resolve the name if it doesn't fit the type.
                // In that case the reference will be marked as invalid, but we can use the fact
                // that a wrong type exists for quick fixes. E.g. change includeGroup to includeConcept.
                element.project.service<JqaRuleIndexingService>().resolve(name)
            }

        return results
            .mapNotNull { definition ->
                definition.computeSource()?.let { PsiElementResolveResult(it, definition.type == jqaRuleType) }
            }.toTypedArray()
    }

    override fun isSoft() = soft
}
