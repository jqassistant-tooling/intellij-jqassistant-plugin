package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import java.io.File

class EffectiveConfigToolWindowFactory : ToolWindowFactory {

    companion object {
        private const val JQA_EFFECTIVE_CONFIG = "jqassistant:effective-configuration"
        private const val GOAL_UNSUCCESSFUL = "Couldn't retrieve data from specified goal"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM) {}

        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()
        showConfig(project, contentManager, contentFactory)
    }

    private fun showConfig(project: Project, contentManager: ContentManager, contentFactory: ContentFactory ){
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Command line tool: Effective Configuration") {
            override fun run(indicator: ProgressIndicator) {
                val output = fetchConfig(project)
                var config = output.substringAfter("Effective configuration for ").substringBefore("[INFO]")

                if(config == "") {
                    config = GOAL_UNSUCCESSFUL
                }

                ApplicationManager.getApplication().invokeLater {
                    val panel = EffectiveConfigurationPanel(config)
                    val content = contentFactory.createContent(panel, "", false)
                    contentManager.addContent(content)
                }
            }
        })
    }


    private fun fetchConfig(project: Project) : String {
        var output = ""
        try {
            val path = project.getBaseDirectories().first().path
            output = CommandLineTool.runMavenGoal(JQA_EFFECTIVE_CONFIG, File(path))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return output
    }
}
