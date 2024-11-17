package org.jqassistant.tooling.intellij.plugin.editor

import com.intellij.openapi.components.service
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import javax.swing.JPanel


import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JTextArea


class TestWindow : ToolWindowFactory {


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM) {}

        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()
        val config = project.service<JqaConfigurationService>().getEffectiveConfig()
        val panel = JPanel().also { it.add(JTextArea(config)) }
        val content = contentFactory.createContent(panel, "Config", false)
        contentManager.addContent(content)
    }


}
