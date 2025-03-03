package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

/**
 * Action to synchronize the [JqaConfigurationService].
 *
 * This is meant to be the user facing sync action of the plugin.
 */
class SynchronizeConfig : AnAction() {
    private var isSynchronizing = false

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (isSynchronizing) return

        isSynchronizing = true
        object : Task.Backgroundable(project, MessageBundle.message("synchronizing.jqa.config")) {
            override fun run(indicator: ProgressIndicator) {
                project.service<JqaConfigurationService>().synchronize()
            }

            override fun onSuccess() {
                isSynchronizing = false
            }

            override fun onCancel() {
                isSynchronizing = false
            }

            override fun onThrowable(error: Throwable) {
                isSynchronizing = false
                super.onThrowable(error)
            }
        }.queue()
    }

    // Set the icon, text and description of the action
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
