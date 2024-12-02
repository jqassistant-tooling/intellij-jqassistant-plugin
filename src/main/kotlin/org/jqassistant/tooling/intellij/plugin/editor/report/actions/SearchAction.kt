package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class SearchAction :
    DumbAwareAction(
        "Search",
        "Search for content ",
        AllIcons.Actions.Search,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        TODO("Proper search not yet implemented")
    }
}
