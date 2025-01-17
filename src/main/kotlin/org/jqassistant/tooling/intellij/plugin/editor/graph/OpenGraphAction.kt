package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.PomTargetPsiElement
import com.intellij.util.xml.DomTarget
import org.jqassistant.tooling.intellij.plugin.common.findRuleById
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class OpenGraphAction : AnAction() {
    companion object {
        const val TOOL_WINDOW_ID: String = "Rule Graph"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val currentElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val domElement = currentElement as? PomTargetPsiElement ?: return

        val target = domElement.target as? DomTarget ?: return
        val ruleBase = target.domElement as? RuleBase ?: return

        val project = event.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return

        val content = toolWindow.contentManager.getContent(0) ?: return
        val component = content.component as? GraphToolWindowContent ?: return

        val configurationService = project.service<JqaConfigurationService>()
        val ruleSet = configurationService.getAvailableRules()

        val currentRule = ruleBase.id.stringValue?.let { ruleSet.findRuleById(it) } ?: return

        component.refreshGraph(currentRule, ruleSet)
    }
}
