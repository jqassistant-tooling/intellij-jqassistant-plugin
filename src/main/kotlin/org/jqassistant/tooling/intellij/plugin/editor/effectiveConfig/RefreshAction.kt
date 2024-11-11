package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

class RefreshAction : ToolWindowAction {
    override fun execute(toolWindow: EffectiveConfigurationToolWindow) {
        toolWindow.refresh()
    }
}