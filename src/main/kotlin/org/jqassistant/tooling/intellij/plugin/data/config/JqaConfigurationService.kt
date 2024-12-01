package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JqaConfigurationService(
    project: Project,
) {
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)
    val configProvider = JqaEffectiveConfigProvider(project, jqaConfigFileProvider)

    init {
        jqaConfigFileProvider.addFileEventListener(configProvider)
    }

    /* Adds a listener that is notified when a config file changes
     *  */
    fun addFileEventListener(listener: EventListener) {
        jqaConfigFileProvider.addFileEventListener(listener)
    }
}
