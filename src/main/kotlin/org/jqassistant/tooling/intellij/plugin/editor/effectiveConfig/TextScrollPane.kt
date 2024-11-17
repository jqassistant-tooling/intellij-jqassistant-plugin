package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.ui.components.JBScrollPane
import javax.swing.JTextPane

class TextScrollPane() : JBScrollPane() {
    private val textPane = JTextPane()

    init {
        this.setViewportView(textPane)
    }
    fun setText(config : String) {
        this.textPane.text = config
    }
}
