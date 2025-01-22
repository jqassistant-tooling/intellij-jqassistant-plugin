package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService

/**
 * Action to synchronize the [JqaConfigurationService].
 *
 * This is meant to be the user facing sync action of the plugin.
 */
class SynchronizeConfig : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            project.service<JqaConfigurationService>().synchronize()
        }
    }
}
