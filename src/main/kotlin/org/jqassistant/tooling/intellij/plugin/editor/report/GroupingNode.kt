package org.jqassistant.tooling.intellij.plugin.editor.report

class GroupingNode(
    private val text: String,
    parent: ReportNode?,
) : ReportNode(parent) {
    override fun toString(): String = text
}
