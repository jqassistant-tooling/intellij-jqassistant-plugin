package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import org.jqassistant.tooling.intellij.plugin.editor.report.ReportToolWindowFactory

class RefreshAction(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : DumbAwareAction(
        "Refresh",
        "Refresh the report from disk",
        AllIcons.Actions.Refresh,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        ReportToolWindowFactory().reloadToolWindow(project, toolWindow)
    }
}
