package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.ui.EditorNotificationPanel
import org.jqassistant.tooling.intellij.plugin.editor.config.SynchronizeConfig

/**
 *  Banner that is shown, when the configuration is outdated
 *  */
class OutdatedConfigBannerPanel(
    private val action: SynchronizeConfig,
) : EditorNotificationPanel(Status.Warning) {
    init {
        text = "Configuration files have changed. Configuration might be outdated."
        createActionLabel(action.templateText ?: "Synchronize") {
            val actionManager = ActionManager.getInstance()
            actionManager.tryToExecute(action, null, null, null, true)
        }
    }
}
