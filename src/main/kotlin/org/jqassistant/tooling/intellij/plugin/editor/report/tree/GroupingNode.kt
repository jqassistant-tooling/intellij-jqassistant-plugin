package org.jqassistant.tooling.intellij.plugin.editor.report.tree

class GroupingNode(
    val text: String,
    parent: ReportNode?,
) : ReportNode(parent) {
    override fun toString(): String = text
}
