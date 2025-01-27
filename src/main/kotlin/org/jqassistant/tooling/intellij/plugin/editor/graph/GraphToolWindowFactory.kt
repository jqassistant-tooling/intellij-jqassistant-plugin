package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

internal class GraphToolWindowFactory :
    ToolWindowFactory,
    DumbAware {
    companion object {
        const val TOOL_WINDOW_ID: String = "Rule Graph"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        loadToolWindowContent(project, toolWindow)
    }

    private fun loadToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = GraphToolWindowContent(project, toolWindow)

        // Add individual tabs for every base directory in the current project that contains a report xml file
        val content =
            ContentFactory.getInstance().createContent(toolWindowContent, "Graph", false)
        toolWindow.contentManager.addContent(content)
    }
}
