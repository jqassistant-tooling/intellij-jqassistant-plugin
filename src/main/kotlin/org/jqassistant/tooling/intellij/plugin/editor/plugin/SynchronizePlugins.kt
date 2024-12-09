package org.jqassistant.tooling.intellij.plugin.editor.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import org.jqassistant.tooling.intellij.plugin.data.plugin.JqaPluginService

class SynchronizePlugins : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            project.service<JqaPluginService>().synchronizePlugins()
        }
    }
}
