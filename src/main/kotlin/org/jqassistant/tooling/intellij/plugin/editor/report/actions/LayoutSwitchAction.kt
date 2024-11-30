package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBSplitter

class LayoutSwitchAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val event = e.inputEvent ?: return
        val source = event.source as ActionButton
        val parent = source.parent as ActionToolbarImpl

        val toolWindowPanel = parent.parent as SimpleToolWindowPanel

        val splitter = toolWindowPanel.content as JBSplitter
        splitter.orientation = !splitter.orientation

        // Switch icon
        source.icon =
            if (splitter.orientation) {
                AllIcons.Actions.SplitVertically
            } else {
                AllIcons.Actions.SplitHorizontally
            }
    }
}
