package org.jqassistant.tooling.intellij.plugin.editor.report

import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.ReferencableRuleType
import org.jqassistant.schema.report.v2.RowType
import java.util.Collections
import java.util.Enumeration
import javax.swing.tree.TreeNode

open class ReportNode(
    private val parent: ReportNode?,
) : TreeNode {
    private val children = mutableListOf<ReportNode>()

    fun addChild(child: ReportNode) {
        children.add(child)
    }

    override fun toString(): String = "child of: $parent"

    override fun getChildAt(index: Int): TreeNode = children[index]

    override fun getChildCount(): Int = children.size

    override fun getParent(): ReportNode? = parent

    override fun getIndex(p0: TreeNode?): Int = children.indexOf(p0)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf(): Boolean = children.size == 0

    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(children)
}

open class ReferencableRuleTypeNode(
    val ref: ReferencableRuleType,
    parent: ReportNode? = null,
) : ReportNode(parent) {
    override fun toString(): String =
        when (ref) {
            is GroupType -> {
                ref.id
            }

            is ConstraintType -> {
                ref.id
            }

            else -> {
                ref.id
            }
        }
}

open class ConstraintResultRowNode(
    val ref: RowType,
    parent: ReportNode?,
) : ReportNode(parent) {
    constructor(ref: RowType) : this(ref, null) {
    }

    override fun toString(): String {
        var str = ""
        for (col in ref.column) str += col.toString()

        return str
    }
}
