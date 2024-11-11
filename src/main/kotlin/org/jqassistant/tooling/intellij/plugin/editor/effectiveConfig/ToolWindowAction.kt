package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

interface ToolWindowAction {
    fun execute(toolWindow: EffectiveConfigurationToolWindow)
}