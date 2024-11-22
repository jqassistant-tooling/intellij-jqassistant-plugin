package org.jqassistant.tooling.intellij.plugin.data.rules

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project

/**
 * Service to retrieve jQA rules of the current project.
 *
 * Under the hood this service manages a list of [JqaRuleIndexingStrategy] and delegates to them. New strategies can be
 * added through their factory and the [JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT] extension point.
 */
@Service(Service.Level.PROJECT)
class JqaRuleIndexingService(
    private val project: Project
) : Disposable, ExtensionPointListener<JqaRuleIndexingStrategyFactory> {
    private val indexes: MutableList<JqaRuleIndexingStrategy> = mutableListOf()

    init {
        JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.addExtensionPointListener(this)
        for (factory in JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.extensions) {
            indexes.add(factory.create(project))
        }
    }

    override fun dispose() {
        JqaRuleIndexingStrategyFactory.Util.EXTENSION_POINT.removeExtensionPointListener(this)
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
    fun resolve(name: String): JqaRuleDefinition? = indexes.firstNotNullOfOrNull { it.resolve(name) }
    fun has(name: String): Boolean = indexes.any { it.has(name) }
}