package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.intellij.ui.components.JBScrollPane
import javax.swing.JTextPane

class TextScrollPane : JBScrollPane() {
    private val textPane = JTextPane()

    init {
        textPane.isEditable = false
        this.setViewportView(textPane)
    }

    fun setText(text: String) {
        this.textPane.text = text
    }
}
