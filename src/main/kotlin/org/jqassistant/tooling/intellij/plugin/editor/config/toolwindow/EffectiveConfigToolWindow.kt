package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jqassistant.tooling.intellij.plugin.data.config.EventListener
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import java.awt.BorderLayout

class EffectiveConfigToolWindow(
    private val project: Project,
) : SimpleToolWindowPanel(false),
    EventListener {
    companion object {
        private const val JQA_EFFECTIVE_CONFIG_GOAL = "jqassistant:effective-configuration"
        private const val GOAL_UNSUCCESSFUL = "Couldn't retrieve data from specified goal"
        private const val PROCESS_TITLE = "Command line tool: Effective Configuration"
    }

    private val myToolBar = EffectiveConfigToolBar(this)
    private val bannerPanel = OutdatedConfigBannerPanel(project, RefreshAction(this))
    private val loadingPanel = LoadingPanel()
    private var currentProgressIndicator: ProgressIndicator? = null
    private val editorFactory = EditorFactory.getInstance()
    private val editor: Editor =
        editorFactory.createEditor(
            editorFactory.createDocument(""),
            project,
            FileTypeManager.getInstance().getFileTypeByExtension("yaml"),
            true,
        )

    init {
        val settings = editor.settings
        settings.isLineNumbersShown = false
        settings.isFoldingOutlineShown = false
        settings.isLineMarkerAreaShown = false
        settings.isIndentGuidesShown = false
        settings.isVirtualSpace = false
        settings.isBlinkCaret = false
        settings.isCaretRowShown = false
        settings.isShowIntentionBulb = false

        this.toolbar = myToolBar.createToolbar()
        bannerPanel.isVisible = false
        add(bannerPanel, BorderLayout.NORTH)
        forceRefresh()
    }

    /**
     *  Refreshes the content of the tool window
     *  */
    private fun updateConfigContent(forceRefresh: Boolean = false) {
        setContent(loadingPanel)
        val configService = project.service<JqaConfigurationService>()
        val config = configService.configProvider.getStoredConfig()

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, PROCESS_TITLE) {
                override fun run(indicator: ProgressIndicator) {
                    currentProgressIndicator?.cancel()
                    var configString =
                        if (config.isValid && !forceRefresh) {
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
                        setEditorContent(configString)
                    }
                }
            },
        )
    }

    /**
     * Sets the text in the document of the editor
     */
    private fun setEditorContent(content: String) {
        val applicationManager = ApplicationManager.getApplication()
        applicationManager.invokeLater {
            applicationManager.runWriteAction {
                editor.document.setText(content.replace("\r", ""))
            }
            setContent(editor.component)
        }
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
