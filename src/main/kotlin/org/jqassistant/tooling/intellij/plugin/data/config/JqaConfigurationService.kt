package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JqaConfigurationService(project: Project) {

    private val jqaEffectiveConfigProvider = JqaEffectiveConfigProvider(project)
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)

    fun getConfigProvider(): JqaEffectiveConfigProvider {
        return jqaEffectiveConfigProvider
    }

    /* Adds a listener that is notified when a config file changes */
    fun addFileEventListener(listener: EventListener) {
        jqaConfigFileProvider.addFileEventListener(listener)
    }
}
