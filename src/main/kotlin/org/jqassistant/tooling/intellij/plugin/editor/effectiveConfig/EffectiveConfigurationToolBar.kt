package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import javax.swing.JComponent

class EffectiveConfigurationToolBar(private val toolWindow: EffectiveConfigurationToolWindow)  {
    fun createToolbar() : JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction(toolWindow))
        }

        val actionManager = ActionManager.getInstance()
        val toolBar = actionManager.createActionToolbar("EffectiveConfigToolBar", actionGroup, true)
        return toolBar.component
    }

}