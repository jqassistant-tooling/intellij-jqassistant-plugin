package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.project.Project
import java.io.File

/** This provides the effective Configuration
To avoid background usage, it is lazy and will NOT automatically fetch the current effective config
Instead, it will check if the config file has been updated by receiving calls from listeners and set the isValid flag accordingly
Shall be used by EffectiveConfigurationToolWindow */

data class Config(
    val configString: String,
    var isValid: Boolean = true,
)

class JqaEffectiveConfigProvider(
    private val project: Project,
    private val configFileProvider: JqaConfigFileProvider,
) : EventListener {
    companion object {
        private const val SUBSTRING_TOP_LEVEL_JQA = "jqassistant"
        private const val SUBSTRING_BEFORE_DELIMITER = "[INFO] Effective configuration for"
        private const val SUBSTRING_AFTER_DELIMITER = "[INFO]"
    }

    private val commandLineTool = CommandLineTool()
    private var config = Config("", false)

    /** Notifies the provider that the config file has changed
     * */
    override fun onEvent() {
        config.isValid = false
    }

    /** Returns the stored configuration, might not be valid at the time of retrieval
     * */
    fun getStoredConfig(): Config = config

    /** Fetches the current effective configuration from the project, this can take a few seconds, make sure to only call from background thread
     * */
    fun getCurrentConfig(): Config {
        config.isValid = false
        configFileProvider.saveDocuments()
        val configString =
            commandLineTool.runMavenGoal("jqassistant:effective-configuration", File(project.basePath.toString()))
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
