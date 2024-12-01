package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService

class EffectiveConfigToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM) {}

        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()
        val panel = EffectiveConfigToolWindow(project)
        val content = contentFactory.createContent(panel, "Config", false)
        contentManager.addContent(content)

        // Be notified when the configuration files change
        val service = project.service<JqaConfigurationService>()
        service.addFileEventListener(panel)
    }
}
