package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent

class EffectiveConfigToolBar(private val toolWindow: EffectiveConfigToolWindow) {
    fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction(toolWindow))
        }

        val actionManager = ActionManager.getInstance()
        val toolBar = actionManager.createActionToolbar("EffectiveConfigToolBar", actionGroup, true)
        toolBar.targetComponent = toolWindow
        return toolBar.component
    }
}
