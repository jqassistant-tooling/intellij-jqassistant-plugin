package org.jqassistant.tooling.intellij.plugin.editor.report.tree

import javax.swing.tree.DefaultTreeModel

class ReportTreeModel(
    root: ReportNode,
) : DefaultTreeModel(root, false) {
    var reverseSorting: Boolean = false
    var searchText: String = ""

    private fun getChildren(parent: Any?): List<ReportNode> {
        val count = super.getChildCount(parent)

        var children = (0..<count).mapNotNull { i -> super.getChild(parent, i) as? ReportNode }

        if (reverseSorting) children = children.reversed()

        if (searchText.isNotEmpty()) {
            children =
                children.filter { node ->
                    if (node.isLeaf) {
                        node.toString().contains(searchText)
                    } else {
                        val enumeration = node.breadthFirstEnumeration()

                        // Skip all grouping nodes
                        var currentElement = enumeration.nextElement()
                        while (currentElement is GroupingNode) {
                            if (!enumeration.hasMoreElements()) return@filter false
                            currentElement = enumeration.nextElement()
                        }

                        val iter = enumeration.toList().toMutableList()
                        // Add back the last skipped node from while loop
                        iter.add(0, currentElement)

                        iter.any { subNode -> subNode.toString().contains(searchText) }
                    }
                }
        }

        return children
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int = getChildren(parent).indexOf(child)

    override fun getChild(parent: Any?, index: Int): Any = getChildren(parent)[index]

    override fun getChildCount(parent: Any?): Int = getChildren(parent).size
}
