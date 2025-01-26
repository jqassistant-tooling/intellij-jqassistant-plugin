package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import org.jqassistant.tooling.intellij.plugin.editor.config.SynchronizeConfig
import javax.swing.JComponent

class EffectiveConfigToolBar(
    private val toolWindow: EffectiveConfigToolWindow,
    private val synchronizeAction: SynchronizeConfig,
) {
    fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup(synchronizeAction)
        val actionManager = ActionManager.getInstance()
        val toolBar = actionManager.createActionToolbar("Configuration Toolbar", actionGroup, true)
        toolBar.targetComponent = toolWindow
        return toolBar.component
    }
}
