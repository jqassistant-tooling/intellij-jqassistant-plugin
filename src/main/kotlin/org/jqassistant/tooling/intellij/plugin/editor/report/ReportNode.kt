package org.jqassistant.tooling.intellij.plugin.editor.report

import org.jqassistant.schema.report.v2.ReferencableRuleType
import javax.swing.tree.DefaultMutableTreeNode

open class ReportNode(
    parent: ReportNode?,
) : DefaultMutableTreeNode(parent) {
    override fun toString(): String = "child of: ${parent as ReportNode}"
}

open class ReferencableRuleTypeNode(
    val ref: ReferencableRuleType,
    parent: ReportNode? = null,
) : ReportNode(parent) {
    override fun toString(): String = ref.id
}
