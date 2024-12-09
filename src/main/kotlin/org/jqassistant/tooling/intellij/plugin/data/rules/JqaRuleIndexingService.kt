package org.jqassistant.tooling.intellij.plugin.data.rules

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project

/**
 * Service to retrieve jQA rules of the current project.
 *
 * Accessing this service should be treated like accessing other indexes, and as such must not happen from the EDT.
 * Consider using [com.intellij.openapi.application.Application.executeOnPooledThread].
 *
 * Under the hood this service manages a list of [JqaRuleIndexingStrategy] and delegates to them. New strategies can be
 * added through their factory and the [JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT] extension point.
 */
@Service(Service.Level.PROJECT)
class JqaRuleIndexingService(
    private val project: Project,
) : ExtensionPointListener<JqaRuleIndexingStrategyFactory> {
    private val indexes: MutableList<JqaRuleIndexingStrategy> = mutableListOf()

    init {
        JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.addExtensionPointListener(this)
        for (factory in JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.extensions) {
            indexes.add(factory.create(project))
        }
    }

    override fun extensionAdded(extension: JqaRuleIndexingStrategyFactory, pluginDescriptor: PluginDescriptor) {
        indexes.add(extension.create(project))
    }

    override fun extensionRemoved(extension: JqaRuleIndexingStrategyFactory, pluginDescriptor: PluginDescriptor) {
        indexes.clear()
        for (factory in JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.extensions) {
            indexes.add(factory.create(project))
        }
    }

    fun getAll(type: JqaRuleType): List<JqaRuleDefinition> = indexes.flatMap { it.getAll(type) }

    fun resolve(identifier: String): List<JqaRuleDefinition> = indexes.flatMap { it.resolve(identifier) }

    fun has(identifier: String): Boolean = indexes.any { it.has(identifier) }
}
