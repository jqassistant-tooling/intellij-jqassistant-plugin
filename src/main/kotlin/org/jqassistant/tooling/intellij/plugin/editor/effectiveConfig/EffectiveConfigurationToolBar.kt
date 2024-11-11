package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import javax.swing.JButton
import javax.swing.JToolBar

class EffectiveConfigurationToolBar(toolWindow: EffectiveConfigurationToolWindow) : JToolBar() {
    init {
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener {
            RefreshAction().execute(toolWindow)
        }

        add(refreshButton)
    }
}