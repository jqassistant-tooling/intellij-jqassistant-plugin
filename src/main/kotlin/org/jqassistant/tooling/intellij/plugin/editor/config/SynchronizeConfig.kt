package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

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

    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation
        val icon = AllIcons.Actions.Refresh
        presentation.icon = icon
        presentation.text = MessageBundle.message("action.synchronizeConfig.text")
        presentation.description = MessageBundle.message("action.synchronizeConfig.description")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT // Most common case: UI updates
    }
}
