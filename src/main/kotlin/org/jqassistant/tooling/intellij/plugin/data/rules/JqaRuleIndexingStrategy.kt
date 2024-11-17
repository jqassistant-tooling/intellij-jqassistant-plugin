package org.jqassistant.tooling.intellij.plugin.data.rules

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

// TODO: Depending on how the indexing strategy for external plugins will work, it might be possible to remove the factory and provide the project as parameter to the methods.
/**
 * A factory that creates a [JqaRuleIndexingStrategy].
 *
 * The factory is used as extension point, so that the plugin can provide the [Project] when actually creating the strategy.
 */
interface JqaRuleIndexingStrategyFactory {
    object Util {
        val EXTENSION_POINT =
            ExtensionPointName.create<JqaRuleIndexingStrategyFactory>("jqassistant.jqaRuleIndexingStrategy")
    }

    fun create(project: Project): JqaRuleIndexingStrategy
}

/**
 * Interface for indexing solutions.
 *
 * One [JqaRuleIndexingStrategy] might use as many indexes as necessary under the hood.
 */
interface JqaRuleIndexingStrategy {
    fun getAll(type: JqaRuleType): List<JqaRuleDefinition>
    fun resolve(name: String): JqaRuleDefinition?
    fun has(name: String): Boolean = resolve(name) != null
}
