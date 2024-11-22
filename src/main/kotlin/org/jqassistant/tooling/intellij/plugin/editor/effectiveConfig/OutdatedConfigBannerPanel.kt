package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel

class OutdatedConfigBannerPanel(private val project: Project, private val action: RefreshAction) :
    EditorNotificationPanel(Status.Warning) {
    init {
        setText("Configuration files have changed. Configuration might be outdated.")
        createActionLabel("Refresh") {
            callAction()
        }
    }

    private fun callAction() {
        val dataContext = DataContext { key ->
            when (key) {
                PlatformDataKeys.PROJECT.name -> project
                else -> null
            }
        }
        val event = AnActionEvent(
            null,
            dataContext,
            "EditorNotificationPanel",
            action.templatePresentation,
            ActionManager.getInstance(),
            0
        )
        action.actionPerformed(event)
    }
}
