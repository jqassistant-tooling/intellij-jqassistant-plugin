package org.jqassistant.tooling.intellij.plugin.editor.report.tree

import javax.swing.tree.DefaultTreeModel

class ReportTreeModel(
    private val originalRoot: ReportNode,
) : DefaultTreeModel(originalRoot, false) {
    var reverseSorting: Boolean = false
    var searchText: String = ""

    /**
     * Swap the root so that the tree model now shows a new tree
     * with the reversing and filter applied
     */
    private fun buildVisibleTree() {
        // insertNodeInto(originalRoot, originalRoot, 0)
    }

    private fun getChildren(parent: Any?): List<ReportNode> {
        val count = super.getChildCount(parent)

        var children = (0..<count).mapNotNull { i -> super.getChild(parent, i) as? ReportNode }

        if (reverseSorting) children = children.reversed()

        if (searchText.isNotEmpty()) {
            children =
                children.filter { node ->
                    if (!node.isLeaf) return@filter true
                    node.toString().startsWith(searchText)
                }
        }

        return children
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int = getChildren(parent).indexOf(child)

    override fun getChild(parent: Any?, index: Int): Any = getChildren(parent)[index]

    override fun getChildCount(parent: Any?): Int = getChildren(parent).size
}
