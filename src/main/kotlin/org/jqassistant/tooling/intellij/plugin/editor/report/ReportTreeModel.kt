package org.jqassistant.tooling.intellij.plugin.editor.report

import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

class ReportTreeModel(
    root: TreeNode,
) : DefaultTreeModel(root, false) {
    var reverseSorting: Boolean = false
    var searchText: String = ""

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        val index = super.getIndexOfChild(parent, child)
        val count = getChildCount(parent)

        return if (reverseSorting) {
            count - (index + 1)
        } else {
            index
        }
    }

    override fun getChild(parent: Any?, index: Int): Any {
        if (reverseSorting) {
            val count = getChildCount(parent)
            return super.getChild(parent, count - (index + 1))
        } else {
            return super.getChild(parent, index)
        }
    }

    override fun getChildCount(parent: Any?): Int {
        if (searchText.isEmpty()) return super.getChildCount(parent)

        return 0
    }
}
