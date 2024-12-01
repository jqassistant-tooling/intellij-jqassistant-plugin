package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jqassistant.tooling.intellij.plugin.editor.report.ReportToolWindowContent

class LayoutSwitchAction(
    private val toolWindowContent: ReportToolWindowContent,
) : DumbAwareAction(
        "Switch Layout",
        "Switch layout between horizontal and vertical splitter",
        AllIcons.Actions.SplitVertically,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        toolWindowContent.splitter.orientation = !toolWindowContent.splitter.orientation
    }
}
