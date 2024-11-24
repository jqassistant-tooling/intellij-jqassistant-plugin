package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class JqaConfigurationService(project: Project) {

    private val jqaEffectiveConfigProvider = JqaEffectiveConfigProvider(project)
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)
    private val fileEventListeners = mutableListOf<FileListener>()


    init {
        println("Service initialized")
        jqaConfigFileProvider.addFileChangeListener(ConfigBulkFileListener::class) {
            jqaEffectiveConfigProvider.onFileUpdate()
            fileEventListeners.forEach { it.onFileChangeEvent() }
        }
    }


    fun getConfigProvider(): JqaEffectiveConfigProvider {
        return jqaEffectiveConfigProvider
    }

    fun addFileEventListener(listener: FileListener) {
        fileEventListeners.add(listener)
    }
}
