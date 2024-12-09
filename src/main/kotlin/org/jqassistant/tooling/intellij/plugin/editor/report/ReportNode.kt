package org.jqassistant.tooling.intellij.plugin.editor.report

import org.jqassistant.schema.report.v2.ReferencableRuleType
import java.util.Collections
import java.util.Enumeration
import javax.swing.tree.TreeNode

open class ReportNode(
    private val parent: ReportNode?,
) : TreeNode {
    private val children = mutableListOf<ReportNode>()

    fun addChild(child: ReportNode) = children.add(child)

    override fun toString(): String = "child of: $parent"

    override fun getChildAt(index: Int): TreeNode = children[index]

    override fun getChildCount(): Int = children.size

    override fun getParent(): ReportNode? = parent

    override fun getIndex(p0: TreeNode?): Int = children.indexOf(p0)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf() = children.size == 0

    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(children)
}

open class ReferencableRuleTypeNode(
    val ref: ReferencableRuleType,
    parent: ReportNode? = null,
) : ReportNode(parent) {
    override fun toString(): String = ref.id
}
