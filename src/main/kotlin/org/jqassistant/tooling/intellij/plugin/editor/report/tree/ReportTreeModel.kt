package org.jqassistant.tooling.intellij.plugin.editor.report.tree

import javax.swing.tree.DefaultTreeModel

class ReportTreeModel(
    private val originalRoot: ReportNode,
) : DefaultTreeModel(originalRoot, false) {
    var reverseSorting: Boolean = false
    var searchText: String = ""
    private val treeCopy: ReportNode

    init {
        treeCopy =
            when (originalRoot) {
                is ReferencableRuleTypeNode ->
                    ReferencableRuleTypeNode(originalRoot.ref, null)

                is GroupingNode ->
                    GroupingNode(originalRoot.text, null)

                else -> throw IllegalArgumentException()
            }

        copyTree(originalRoot, treeCopy)
    }

    private fun copyTree(oldRoot: ReportNode, newRoot: ReportNode) {
        for (child in oldRoot.children()) {
            val newChild =
                when (child) {
                    is ReferencableRuleTypeNode ->
                        ReferencableRuleTypeNode(child.ref, null)

                    is GroupingNode ->
                        GroupingNode(child.text, null)

                    else -> throw IllegalArgumentException()
                }

            copyTree(child as ReportNode, newChild)
            newRoot.add(newChild)
        }
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
