package org.jqassistant.tooling.intellij.plugin.common

import com.buschmais.jqassistant.core.rule.api.filter.RuleFilter

/**
 * Utilities to work with jQA Wildcards.
 *
 * If possible uses jQA logic but provides some additional functionality based on jQA implementation details.
 */
object WildcardUtil {
    /**
     * Checks whether [ruleId] matches the given [wildcard].
     *
     * Implementation is fully compliant with jQA.
     */
    fun matches(ruleId: String, wildcard: String) = RuleFilter.matches(ruleId, wildcard)

    /**
     * Determines whether a [ruleId] looks like a jQA Wildcard.
     *
     * Implementation is dependent on jQA implementation details since jQA doesn't expose anything like this.
     */
    fun looksLikeWildcard(ruleId: String) = "*" in ruleId || "?" in ruleId

    /**
     * Converts wildcard [ruleId] to regular id by stripping characters.
     *
     * Implementation is dependent on jQA implementation details since jQA doesn't expose anything like this.
     */
    fun stripWildcard(ruleId: String) = ruleId.replace("*", "").replace("?", "")
}
