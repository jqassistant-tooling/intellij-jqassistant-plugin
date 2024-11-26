package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jqassistant.tooling.intellij.plugin.data.config.Config
import org.jqassistant.tooling.intellij.plugin.data.config.EventListener
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import java.awt.BorderLayout

class EffectiveConfigToolWindow(private val project: Project) : SimpleToolWindowPanel(false), EventListener {

    companion object {
        private const val JQA_EFFECTIVE_CONFIG_GOAL = "jqassistant:effective-configuration"
        private const val GOAL_UNSUCCESSFUL = "Couldn't retrieve data from specified goal"
        private const val PROCESS_TITLE = "Command line tool: Effective Configuration"
    }

    private val myToolBar = EffectiveConfigToolBar(this)
    private val textPane = TextScrollPane()
    private val bannerPanel = OutdatedConfigBannerPanel(project, RefreshAction(this))
    private val loadingPanel = LoadingPanel()


    init {
        this.toolbar = myToolBar.createToolbar()
        bannerPanel.isVisible = false
        add(bannerPanel, BorderLayout.NORTH)
        fullRefresh()
    }

    /** Refreshes the content of the tool window */
    private fun refreshConfigContent() {
        setContent(loadingPanel)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, PROCESS_TITLE) {
            override fun run(indicator: ProgressIndicator) {
                val configService = project.service<JqaConfigurationService>()
                val storedConfig = configService.getConfigProvider().getStoredConfig()
                val config: Config = if (storedConfig.isValid) {
                    storedConfig
                } else {
                    configService.getConfigProvider().fetchCurrentConfig()
                }
                var text = config.configString
                if (text.isEmpty()) {
                    text += "$GOAL_UNSUCCESSFUL: \"$JQA_EFFECTIVE_CONFIG_GOAL\""
                }
                textPane.setText(text)
                setContent(textPane)
            }
        })
    }

    fun fullRefresh() {
        refreshConfigContent().also { bannerPanel.isVisible = false }
    }

    // Be notified when the config file changes
    override fun onEvent() {
        bannerPanel.isVisible = true
        revalidate()
        repaint()
    }
}
