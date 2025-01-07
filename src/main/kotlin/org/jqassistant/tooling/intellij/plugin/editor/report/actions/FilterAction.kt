package org.jqassistant.tooling.intellij.plugin.editor.report.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getTreePath
import com.intellij.ui.treeStructure.Tree
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportNode
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportTreeModel

class FilterAction(
    private val trees: List<Tree>,
) : DumbAwareAction(
        "Filter",
        "Filter report",
        AllIcons.General.Filter,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        for (tree in trees) {
            // Save all expanded paths
            val root = tree.model.root as ReportNode
            val expandedPaths = tree.getExpandedDescendants(tree.model.getTreePath(root.userObject))

            val model = tree.model as? ReportTreeModel ?: continue
            // Notify tree so repaint happens
            model.reload()

            // Re-expand all previously expanded paths
            for (path in expandedPaths) {
                tree.expandPath(path)
            }
        }
    }
}
