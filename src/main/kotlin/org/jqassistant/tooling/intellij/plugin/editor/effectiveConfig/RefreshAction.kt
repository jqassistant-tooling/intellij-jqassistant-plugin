package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class RefreshAction(private val toolWindow: EffectiveConfigToolWindow) : DumbAwareAction("Refresh","Refresh effective config", AllIcons.General.InlineRefresh ) {
    override fun actionPerformed(p0: AnActionEvent) {
        toolWindow.fullRefresh()
    }
}
