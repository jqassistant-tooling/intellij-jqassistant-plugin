package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.project.Project
import java.io.File

/** This provides the effective Configuration
To avoid background usage, it will not automatically fetch the current effective config
Instead, it will check if the config file has been updated by receiving calls from listeners and set the isValid flag accordingly
Shall be used by EffectiveConfigurationToolWindow */

data class Config(val configString: String, var isValid: Boolean = true)

class JqaEffectiveConfigProvider(private val project: Project) {

    companion object {
        private const val SUBSTRING_TOP_LEVEL_JQA = "jqassistant"
        private const val SUBSTRING_BEFORE_DELIMITER = "[INFO] Effective configuration for"
        private const val SUBSTRING_AFTER_DELIMITER = "[INFO]"
    }

    private val commandLineTool = CommandLineTool()
    private var config = Config("", false)

    init {
        onFileUpdate()
    }

    fun onFileUpdate() {
        config.isValid = false

    }

    fun getStoredConfig(): Config {
        return config
    }

    /** Fetches the current effective configuration from the project, this can take a few seconds, make sure to only call from background thread
     * */
    fun fetchCurrentConfig(): Config {
        val configString =
            commandLineTool.runMavenGoal("jqassistant:effective-configuration", File(project.basePath.toString()))
        config.isValid = false
        config = Config(stripConfig(configString))
        return config
    }

    private fun stripConfig(text: String): String {
        var result = text.substringAfter(SUBSTRING_BEFORE_DELIMITER)
        val index = result.indexOf(SUBSTRING_TOP_LEVEL_JQA)
        if (index != -1) {
            result = result.substring(index)
        }
        return result.substringBefore(SUBSTRING_AFTER_DELIMITER)
    }


}
