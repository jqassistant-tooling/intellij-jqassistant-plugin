package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JToolBar
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
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