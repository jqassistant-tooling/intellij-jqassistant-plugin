package org.jqassistant.tooling.intellij.plugin.editor.plugin

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import org.jqassistant.tooling.intellij.plugin.common.notifyBalloon
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.plugin.JqaPluginService
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class SynchronizePlugins : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val config = project.service<JqaConfigurationService>().getConfiguration()
            if (config == null) {
                project.notifyBalloon(
                    MessageBundle.message("synchronizing.jqa.plugins.no.config"),
                    NotificationType.ERROR,
                )
                return@executeOnPooledThread
            }
            project.service<JqaPluginService>().synchronizePlugins(config)
        }
    }
}
