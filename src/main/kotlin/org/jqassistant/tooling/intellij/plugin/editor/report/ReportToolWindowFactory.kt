package org.jqassistant.tooling.intellij.plugin.editor.report;

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jqassistant.tooling.intellij.plugin.data.ReportProviderService


internal class ReportToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reportProviderService = project.service<ReportProviderService>()

        for ((baseDir, report) in reportProviderService.readReports()) {
            val toolWindowContent = ReportToolWindowContent(project, baseDir, report)

            // Add individual tabs for every base directory in the current project that contains a report xml file
            val content =
                ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, baseDir.name, false)
            toolWindow.contentManager.addContent(content)
        }
    }
}
