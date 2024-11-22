package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.*

/** This provides the effective Configuration
To avoid background usage, it will not automatically fetch the current effective config
Instead, it will check if the config file has been updated by receiving calls from listeners and set the isValid flag accordingly
Shall be used by EffectiveConfigurationToolWindow */

data class Config(val config: String, val timestamp: Date, var isValid: Boolean = true)

class JqaEffectiveConfigProvider(private val project: Project) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val delayTime = 10000L
    private var config = Config("", Date())

    init {
        onFileUpdate()
    }

    fun onFileUpdate() {
        job?.cancel()
        job = coroutineScope.launch {
            delay(delayTime)
            setLastChanged()
        }
    }

    fun getStoredConfig(): Config {
        return this.config
    }

    fun fetchCurrentConfig() {
        //val configString = CommandLineTool.fetchConfig(project, "jqassistant:effective-configuration")
        //val config.isValid = false
        //val timestamp = Date()
        //config = Config(configString, timestamp)
    }

    private fun setLastChanged() {
        val lastChanged = Date()
        if (lastChanged.time != config.timestamp.time) {
            config.isValid = false
        }
    }


}
