package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel

class OutdatedConfigBannerPanel(private val project: Project, private val action: RefreshAction) :
    EditorNotificationPanel(Status.Warning) {
    init {
        setText("Configuration files have changed. Configuration might be outdated.")
        createActionLabel("Refresh") {
            callRefreshAction()
        }
    }

    private fun callRefreshAction() {
        val dataContext = DataContext { key ->
            when (key) {
                PlatformDataKeys.PROJECT.name -> project
                else -> null
            }
        }

        val presentation = Presentation().apply { copyFrom(action.templatePresentation) }

        val event = AnActionEvent(
            null,
            dataContext,
            "EditorNotificationPanel",
            presentation,
            ActionManager.getInstance(),
            0
        )
        action.actionPerformed(event)
        this.isVisible = false
    }
}
