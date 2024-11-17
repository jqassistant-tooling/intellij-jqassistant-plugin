package org.jqassistant.tooling.intellij.plugin.editor.report


import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import org.jqassistant.schema.report.v2.ConceptType
import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.StatusEnumType
import javax.swing.JTree

class ReportCellRenderer() : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ) {
        if (value == null) {
            append("NULL")
            return
        }
        if (value !is ReportNode) {
            append("INVALID")
            return
        }

        val reportNode = value as ReportNode



        when (reportNode) {
            is ReferencableRuleTypeNode -> {
                val rule = reportNode.ref
                when (rule) {
                    is GroupType -> {
                        icon = AllIcons.General.InspectionsOKEmpty
                        append(rule.id)
                    }

                    is ConstraintType -> {
                        icon = when (rule.status) {
                            StatusEnumType.SUCCESS -> AllIcons.RunConfigurations.TestPassed
                            StatusEnumType.FAILURE -> AllIcons.RunConfigurations.TestFailed
                            StatusEnumType.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
                            StatusEnumType.WARNING -> AllIcons.RunConfigurations.TestCustom
                        }
                        append(rule.id)
                    }

                    is ConceptType -> {
                        icon = when (rule.status) {
                            StatusEnumType.SUCCESS -> AllIcons.RunConfigurations.TestPassed
                            StatusEnumType.FAILURE -> AllIcons.RunConfigurations.TestFailed
                            StatusEnumType.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
                            StatusEnumType.WARNING -> AllIcons.RunConfigurations.TestCustom
                        }
                        append("C: ${rule.id}")
                    }

                    else -> {
                        icon = AllIcons.RunConfigurations.TestUnknown
                        append(rule.id)
                    }
                }
            }
        }
    }
}
