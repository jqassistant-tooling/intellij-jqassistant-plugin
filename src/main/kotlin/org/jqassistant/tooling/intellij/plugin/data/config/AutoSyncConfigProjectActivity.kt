package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jqassistant.tooling.intellij.plugin.data.plugin.JqaPluginService
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class AutoSyncConfigProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        object : Task.Backgroundable(project, MessageBundle.message("synchronizing.jqa.config")) {
            override fun run(indicator: ProgressIndicator) {
                // Ensure the plugin service is instantiated and listening to configuration updates
                project.service<JqaPluginService>()

                val configurationService = project.service<JqaConfigurationService>()
                configurationService.synchronize()
            }
        }.queue()
    }
}
