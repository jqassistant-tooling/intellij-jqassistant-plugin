package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jqassistant.tooling.intellij.plugin.data.plugin.JqaPluginService
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class AutoSyncConfigProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<JqaPluginService>()

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, MessageBundle.message("synchronizing.jqa.config"), true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.text = MessageBundle.message("synchronizing.jqa.config")
                    indicator.isIndeterminate = true

                    // Wait for indexing done so that (Maven) project is available to resolve configuration
                    DumbService.getInstance(project).waitForSmartMode()
                    indicator.checkCanceled()

                    // load initial config as previous update events are not sticky, thus not consumed during init
                    val configurationService = project.service<JqaConfigurationService>()

                    configurationService.synchronize()
                }

                override fun onCancel() {
                    thisLogger().info("jQAssistant configuration cancelled by user.")
                }

                override fun onThrowable(error: Throwable) {
                    thisLogger().warn("jQAssistant configuration failed.", error)
                }
            },
        )
    }
}
