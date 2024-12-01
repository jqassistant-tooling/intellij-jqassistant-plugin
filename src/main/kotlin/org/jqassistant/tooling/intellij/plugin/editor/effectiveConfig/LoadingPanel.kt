package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel

class LoadingPanel : JPanel() {
    init {
        layout = BorderLayout()
        val loadingIcon = ImageIcon(javaClass.getResource("/icons/loading.gif"))
        val loadingLabel = JLabel("Loading...", loadingIcon, JLabel.CENTER)
        add(loadingLabel, BorderLayout.CENTER)
    }
}
