package org.jqassistant.tooling.intellij.plugin.data.rules

import com.intellij.psi.PsiElement

enum class JqaRuleType {
    CONCEPT,
    CONSTRAINT,
    GROUP,
}

/**
 * Contains information about a jQA rule, including its characteristics like [name] and [type], but also its declaration
 * site.
 *
 * How the declaration is stored is up to the implementation, but it must be resolved to an [PsiElement] when
 * [computeSource] is called.
 */
abstract class JqaRuleDefinition(
    val name: String,
    val type: JqaRuleType,
) {
    abstract fun computeSource(): PsiElement?
}

/**
 * [JqaRuleDefinition] that contains a psi element as value.
 */
class ValueBasedJqaRuleDefinition(
    name: String,
    type: JqaRuleType,
    private val element: PsiElement? = null,
) : JqaRuleDefinition(name, type) {
    override fun computeSource(): PsiElement? = element
}
