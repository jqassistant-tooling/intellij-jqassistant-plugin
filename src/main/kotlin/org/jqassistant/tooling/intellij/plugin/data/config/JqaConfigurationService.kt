package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class JqaConfigurationService(project: Project) :  FileListener{

    private val jqaEffectiveConfigProvider = JqaEffectiveConfigProvider(project)
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)
    private val fileEventListeners = mutableListOf<FileListener>()


    init {
        jqaConfigFileProvider.addFileListener(this)
    }

    fun getConfigProvider(): JqaEffectiveConfigProvider {
        return jqaEffectiveConfigProvider
    }

    fun addFileEventListener(listener: FileListener) {
        fileEventListeners.add(listener)
    }

    override fun onFileChangeEvent() {
        fileEventListeners.forEach { it.onFileChangeEvent() }
    }
}
