package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.treeStructure.Tree
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportTreeModel

class SearchAction(
    private val trees: List<Tree>,
) : DumbAwareAction(
        "Search",
        "Search for content ",
        AllIcons.Actions.Search,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        for (tree in trees) {
            val model = tree.model as? ReportTreeModel ?: continue
            // Notify tree so repaint happens
            model.reload()
        }
    }
}
