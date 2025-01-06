package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.treeStructure.Tree
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportTreeModel

class SortingToggleAction(
    private val trees: List<Tree>,
) : DumbAwareAction(
        "Switch Sorting Direction",
        "Switch sorting direction between alphabetically ascending and descending order",
        // This should be AllIcons.Expui.ObjectBrowser.SortAlphabetically, but that is not available yet
        AllIcons.RunConfigurations.SortbyDuration,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        for (tree in trees) {
            val model = tree.model as? ReportTreeModel ?: continue
            model.reverseSorting = !model.reverseSorting

            // Notify tree so repaint happens
            model.reload()
        }
    }
}
