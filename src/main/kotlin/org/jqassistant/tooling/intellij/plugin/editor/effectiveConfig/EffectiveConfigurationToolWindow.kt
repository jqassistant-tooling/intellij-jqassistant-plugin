package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

class EffectiveConfigurationToolWindow(private val project: Project) : JPanel() {

    companion object {
        private const val JQA_EFFECTIVE_CONFIG_GOAL = "jqassistant:effective-configuration"
        private const val GOAL_UNSUCCESSFUL = "Couldn't retrieve data from specified goal"
        private const val TOP_LEVEL_JQA_NAMESPACE = "jqassistant"
    }

    private var toolBar : EffectiveConfigurationToolBar = EffectiveConfigurationToolBar(this)
    private var contentArea : EffectiveConfigurationScrollPane = EffectiveConfigurationScrollPane()

    init {
        this.add(toolBar, BorderLayout.NORTH)
        this.add(contentArea, BorderLayout.CENTER)
        this.refreshConfigContent(project)
    }

    private fun refreshConfigContent(project: Project){
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Command line tool: Effective Configuration") {
            override fun run(indicator: ProgressIndicator) {
                var config = fetchConfig(project, JQA_EFFECTIVE_CONFIG_GOAL)

                if(config == "") {
                    config = "$GOAL_UNSUCCESSFUL: \"$JQA_EFFECTIVE_CONFIG_GOAL\""
                }
                contentArea.setText(config)
            }
        })
    }


    private fun fetchConfig(project: Project, goal: String) : String {
        var output = ""
        try {
            val path = project.getBaseDirectories().first().path
            output = CommandLineTool.runMavenGoal(goal, File(path))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stripConfig(output)
    }

    private fun stripConfig(text: String) : String {
        var result = text.substringAfter("[INFO] Effective configuration for")
        val index = result.indexOf(TOP_LEVEL_JQA_NAMESPACE)
        if (index != -1) {
            result = result.substring(index)
        }
        return result.substringBefore("[INFO]")
    }

    fun refresh() {
        refreshConfigContent(project)
    }
}