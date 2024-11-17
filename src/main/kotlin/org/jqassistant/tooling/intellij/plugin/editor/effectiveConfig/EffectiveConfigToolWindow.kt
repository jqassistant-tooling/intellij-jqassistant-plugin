package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import java.io.File

class EffectiveConfigToolWindow(private val project: Project) : SimpleToolWindowPanel(false) {

    companion object {
        private const val JQA_EFFECTIVE_CONFIG_GOAL = "jqassistant:effective-configuration"
        private const val GOAL_UNSUCCESSFUL = "Couldn't retrieve data from specified goal"
        private const val PROCESS_TITLE = "Command line tool: Effective Configuration"
        private const val SUBSTRING_TOP_LEVEL_JQA = "jqassistant"
        private const val SUBSTRING_BEFORE_DELIMITER = "[INFO] Effective configuration for"
        private const val SUBSTRING_AFTER_DELIMITER = "[INFO]"
    }

    private val myToolBar = EffectiveConfigToolBar(this)
    private val textPane = TextScrollPane()

    init {

        toolbar = myToolBar.createToolbar()
        setContent(textPane)
        fullRefresh()
    }

    private fun refreshConfigContent(){
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, PROCESS_TITLE) {
            override fun run(indicator: ProgressIndicator) {
                var config = fetchConfig(project, JQA_EFFECTIVE_CONFIG_GOAL)
                if(config == "") config = "$GOAL_UNSUCCESSFUL: \"$JQA_EFFECTIVE_CONFIG_GOAL\""
                textPane.setText(config)
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
        var result = text.substringAfter(SUBSTRING_BEFORE_DELIMITER)
        val index = result.indexOf(SUBSTRING_TOP_LEVEL_JQA)
        if (index != -1) {
            result = result.substring(index)
        }
        return result.substringBefore(SUBSTRING_AFTER_DELIMITER)
    }

    fun fullRefresh() {
        refreshConfigContent()
    }
}
