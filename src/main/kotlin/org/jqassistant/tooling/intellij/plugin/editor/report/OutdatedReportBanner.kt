package org.jqassistant.tooling.intellij.plugin.editor.report

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.EditorNotificationPanel
import org.jqassistant.tooling.intellij.plugin.data.report.ReportProviderService

class OutdatedReportBanner(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : EditorNotificationPanel(Status.Warning) {
    init {
        isVisible = false
        createActionLabel("Refresh") {
            refreshReport()
        }

        val reportProviderService = project.service<ReportProviderService>()
        reportProviderService.onReportChange(this::makeVisible)
    }

    private fun makeVisible(rootDirectory: VirtualFile) {
        this.text = "Report in ${rootDirectory.name} has changed. Shown report might be outdated"
        this.isVisible = true
    }

    /**
     * Refreshes the toolwindow content
     * */
    private fun refreshReport() {
        ReportToolWindowFactory().reloadToolWindow(project, toolWindow)
        this.isVisible = false
    }
}
