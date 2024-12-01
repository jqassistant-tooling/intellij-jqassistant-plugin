package org.jqassistant.tooling.intellij.plugin.editor.report

class DividerNode(
    parent: ReportNode?,
) : ReportNode(parent) {
    override fun toString(): String = "divider"
}
