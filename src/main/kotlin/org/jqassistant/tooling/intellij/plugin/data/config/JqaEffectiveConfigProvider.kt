package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.coroutineContext

// Will fetch and hold the current effective config and shall be called whenever config files are updated
// Shall be used by EffectiveConfigurationToolWindow

class JqaEffectiveConfigProvider(private val project: Project) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val delayTime = 10000L
    private var configString = "test"

    init {
        updateConfig()
    }

    fun updateConfig() {
        job?.cancel()
        job = coroutineScope.launch {
            delay(delayTime)
            fetchConfig()
        }
    }

    fun getConfig(): String {
        return this.configString
    }

    private fun fetchConfig() {
        //configString = CommandLineTool.fetchConfig(project, "jqassistant:effective-configuration")
        println("Updated effective config")

    }
}
