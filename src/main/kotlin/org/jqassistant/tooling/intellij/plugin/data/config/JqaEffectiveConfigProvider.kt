package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.project.Project

// Will fetch and hold the current effective config and shall be called whenever config files are updated
// Shall be used by EffectiveConfigurationToolWindow

class JqaEffectiveConfigProvider(private val project: Project) {

    private var configString = "test"

    init {
        updateConfig()
    }

    fun updateConfig() {
        fetchConfig()
    }

    fun getConfig(): String {
        return this.configString
    }

    private fun fetchConfig() {
        //configString = CommandLineTool.fetchConfig(project, "jqassistant:effective-configuration")

    }
}
