package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.ui.components.JBScrollPane
import javax.swing.JTextPane

class EffectiveConfigurationPanel(config : String) : JBScrollPane() {
    init {
        val text = JTextPane()
        text.text = config
        this.setViewportView(text)
    }
}