package org.jqassistant.tooling.intellij.plugin.editor.report


import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.ReferencableRuleType
import org.jqassistant.schema.report.v2.RowType
import java.util.*
import javax.swing.tree.TreeNode
import kotlin.collections.ArrayList


/**
 * Extension function for converting a {@link List} to an {@link Enumeration}
 */
fun <T> List<T>.toEnumeration(): Enumeration<T> {
    return object : Enumeration<T> {
        var count = 0

        override fun hasMoreElements(): Boolean {
            return this.count < size
        }

        override fun nextElement(): T {
            if (this.count < size) {
                return get(this.count++)
            }
            throw NoSuchElementException("List enumeration asked for more elements than present")
        }
    }
}

open class ReportNode(val daddy: ReportNode?) : TreeNode {
    val children: ArrayList<ReportNode> = ArrayList()

    fun addChild(child: ReportNode) {
        children.add(child)
    }

    override fun toString(): String {
        return "child of: $daddy"
    }

    override fun getChildAt(p0: Int): TreeNode {
        return children.get(p0)
    }

    override fun getChildCount(): Int {
        return children.size
    }

    override fun getParent(): ReportNode? {
        return daddy
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
        return children.toEnumeration()
    }

}

open class ReferencableRuleTypeNode(val ref: ReferencableRuleType, daddy: ReportNode?) : ReportNode(daddy) {
    constructor(ref: ReferencableRuleType) : this(ref, null) {
    }

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

open class ConstraintResultRowNode(val ref: RowType, daddy: ReportNode?) : ReportNode(daddy) {
    constructor(ref: RowType) : this(ref, null) {
    }

    override fun toString(): String {
        var str = ""
        for (col in ref.column) str += col.toString()

        return str
    }
}