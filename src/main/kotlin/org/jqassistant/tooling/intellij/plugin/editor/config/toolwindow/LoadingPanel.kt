package org.jqassistant.tooling.intellij.plugin.editor.config.toolwindow

import com.intellij.ui.AnimatedIcon
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class LoadingPanel : JPanel() {
    init {
        layout = BorderLayout()
        val loadingIcon = AnimatedIcon.Default()
        val loadingLabel = JLabel("Loading...", loadingIcon, JLabel.CENTER)
        add(loadingLabel, BorderLayout.CENTER)
    }
}
