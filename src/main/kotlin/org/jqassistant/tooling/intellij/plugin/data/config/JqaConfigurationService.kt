package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class JqaConfigurationService(private val project: Project) {

    private val jqaEffectiveConfigProvider = JqaEffectiveConfigProvider(project)
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)


    init {
        println("Service initialized")
        jqaConfigFileProvider.addFileChangeListener(FileChangedListener::class) {
            // TODO only update after some time with no further changes, as there might be a lot of incoming events at once
            // TODO give user the option to load the effective config manually after we registered a change
            jqaEffectiveConfigProvider.onFileUpdate()
        }
    }

    fun getEffectiveConfig(): Config {
        return jqaEffectiveConfigProvider.getStoredConfig()
    }

    fun getAllConfigurationFiles(): List<VirtualFile> {
        return jqaConfigFileProvider.getFiles()
    }
}
