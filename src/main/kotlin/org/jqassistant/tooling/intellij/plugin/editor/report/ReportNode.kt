package org.jqassistant.tooling.intellij.plugin.editor.report


import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.ReferencableRuleType
import org.jqassistant.schema.report.v2.ResultType
import org.jqassistant.schema.report.v2.RowType
import java.util.*
import javax.swing.tree.TreeNode

open class ReportNode(private val parent: ReportNode?) : TreeNode {
    private val children = mutableListOf<ReportNode>();

    fun addChild(child: ReportNode) {
        children.add(child)
    }

    override fun toString(): String {
        return "child of: $parent"
    }

    override fun getChildAt(index: Int): TreeNode {
        return children[index]
    }

    override fun getChildCount(): Int {
        return children.size
    }

    override fun getParent(): ReportNode? {
        return parent
    }

    override fun getIndex(p0: TreeNode?): Int {
        return children.indexOf(p0)
    }

    override fun getAllowsChildren(): Boolean {
        return true
    }

    override fun isLeaf(): Boolean {
        return children.size == 0
    }

    override fun children(): Enumeration<out TreeNode> {
        return Collections.enumeration(children)
    }

}

open class ReferencableRuleTypeNode(val ref: ReferencableRuleType, parent: ReportNode? = null) : ReportNode(parent) {
    override fun toString(): String {
        return when (ref) {
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
}
