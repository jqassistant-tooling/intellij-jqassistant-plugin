package org.jqassistant.tooling.intellij.plugin.editor.report

import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

class ReportTreeModel(
    root: TreeNode,
) : DefaultTreeModel(root, false) {
    var reverseSorting: Boolean = false

    override fun getChild(parent: Any?, index: Int): Any {
        if (reverseSorting) {
            val count = getChildCount(parent)
            return super.getChild(parent, count - (index + 1))
        } else {
            return super.getChild(parent, index)
        }
    }
}
