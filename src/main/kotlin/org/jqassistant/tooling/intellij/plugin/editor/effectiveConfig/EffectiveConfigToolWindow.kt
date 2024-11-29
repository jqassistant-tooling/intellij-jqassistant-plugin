package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
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
    private var currentProgressIndicator: ProgressIndicator? = null

    init {
        this.toolbar = myToolBar.createToolbar()
        bannerPanel.isVisible = false
        add(bannerPanel, BorderLayout.NORTH)
        forceRefresh()
    }

    /** Refreshes the content of the tool window */
    private fun updateConfigContent(forceRefresh: Boolean = false) {
        setContent(loadingPanel)
        val configService = project.service<JqaConfigurationService>()
        val config = configService.configProvider.getStoredConfig()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, PROCESS_TITLE) {
            override fun run(indicator: ProgressIndicator) {
                currentProgressIndicator?.cancel()
                var configString = if (config.isValid && !forceRefresh) {
                    config.configString
                } else {
                    currentProgressIndicator = indicator
                    val newConfig = configService.configProvider.getCurrentConfig()
                    newConfig.configString
                }

                if (currentProgressIndicator == null || !currentProgressIndicator!!.isCanceled) {
                    if (configString.isEmpty()) {
                        configString += "$GOAL_UNSUCCESSFUL: \"$JQA_EFFECTIVE_CONFIG_GOAL\""
                    }
                    textPane.setText(configString)
                    setContent(textPane)
                }
            }
        })
    }

    fun forceRefresh() {
        updateConfigContent(true).also { bannerPanel.isVisible = false }
    }

    /** Be notified when the config file changes
    */
    override fun onEvent() {
        bannerPanel.isVisible = true
        revalidate()
        repaint()
    }
}
