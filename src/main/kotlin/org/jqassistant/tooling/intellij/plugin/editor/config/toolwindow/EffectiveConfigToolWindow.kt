package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.buschmais.jqassistant.core.shared.configuration.ConfigurationSerializer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jqassistant.tooling.intellij.plugin.data.config.FullArtifactConfiguration
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.config.JqaSyncListener
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import org.jqassistant.tooling.intellij.plugin.editor.config.SynchronizeConfig
import java.awt.BorderLayout
import javax.swing.JLabel

class EffectiveConfigToolWindow(
    private val project: Project,
) : SimpleToolWindowPanel(false),
    JqaSyncListener {
    private val synchronizeAction = SynchronizeConfig()
    private val myToolBar = EffectiveConfigToolBar(this, synchronizeAction)
    private val editorFactory = EditorFactory.getInstance()
    private val editor: Editor =
        editorFactory.createEditor(
            editorFactory.createDocument(""),
            project,
            FileTypeManager.getInstance().getFileTypeByExtension("yaml"),
            true,
        )

    // Currently unused
    private val bannerPanel = OutdatedConfigBannerPanel(synchronizeAction)

    init {
        // Listen to changes in the configuration
        project.messageBus.connect().subscribe(JqaSyncListener.TOPIC, this)

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
        updateConfigContent()
    }

    /**
     *  Refreshes the content of the tool window
     *  */
    private fun updateConfigContent() {
        val applicationManager = ApplicationManager.getApplication()
        applicationManager.invokeLater {
            applicationManager.runWriteAction {
                val configService = project.service<JqaConfigurationService>()
                val config = configService.getConfiguration()
                if (config == null) {
                    setContent(JLabel(MessageBundle.message("configuration.toolwindow.error.null")))
                } else {
                    val configString = ConfigurationSerializer<FullArtifactConfiguration>().toYaml(config)
                    setEditorContent(configString)
                }
            }
        }
    }

    private fun setEditorContent(content: String) {
        val correctedFormat = content.replace("\r", "")
        editor.document.setText(correctedFormat)
        setContent(editor.component)
    }

    override fun synchronize(config: FullArtifactConfiguration?) {
        updateConfigContent()
    }
}
