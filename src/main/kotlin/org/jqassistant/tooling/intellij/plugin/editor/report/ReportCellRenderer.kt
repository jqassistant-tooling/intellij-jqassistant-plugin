package org.jqassistant.tooling.intellij.plugin.editor.report


import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
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
                when (val rule = reportNode.ref) {
                    is GroupType -> {
                        icon = AllIcons.Nodes.Folder
                        append(rule.id)
                    }

                    is ConstraintType -> {
                        icon = when (rule.status) {
                            StatusEnumType.SUCCESS -> AllIcons.RunConfigurations.TestPassed
                            StatusEnumType.FAILURE -> AllIcons.RunConfigurations.TestFailed
                            StatusEnumType.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
                            StatusEnumType.WARNING -> AllIcons.RunConfigurations.TestCustom
                        }

                        val text = rule.id
                        when (rule.severity.value) {
                            "major" -> append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                            else -> append(text)
                        }

                        toolTipText = rule.description
                    }

                    is ConceptType -> {
                        icon = when (rule.status) {
                            StatusEnumType.SUCCESS -> AllIcons.RunConfigurations.TestPassed
                            StatusEnumType.FAILURE -> AllIcons.RunConfigurations.TestFailed
                            StatusEnumType.SKIPPED -> AllIcons.RunConfigurations.TestSkipped
                            StatusEnumType.WARNING -> AllIcons.RunConfigurations.TestCustom
                        }


                        val text = rule.id
                        when (rule.severity.value) {
                            "major" -> append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                            else -> append(text)
                        }

                        toolTipText = rule.description
                    }

                    else -> {
                        icon = AllIcons.RunConfigurations.TestUnknown
                        append(rule.id)
                    }
                }
            }

            else -> {
                append("Concepts:")
            }
        }
    }
}
