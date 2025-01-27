package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.xml.DomTarget
import org.jqassistant.tooling.intellij.plugin.common.findRuleById
import org.jqassistant.tooling.intellij.plugin.common.notifyBalloon
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class OpenGraphAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        fun invalidRuleNotification() {
            project.notifyBalloon(
                MessageBundle.message("graph.invalid.selection"),
                NotificationType.ERROR,
            )
        }

        val currentElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return invalidRuleNotification()

        // When clicking on the id directly in the rule definition then the element will be a [PomTargetPsiElement]
        // otherwise it will be a [XmlAttributeValue]
        val ruleId =
            when (currentElement) {
                is XmlAttributeValue -> {
                    currentElement.value
                }

                is PomTargetPsiElement -> {
                    val target = currentElement.target as? DomTarget ?: return invalidRuleNotification()
                    val ruleBase = target.domElement as? RuleBase ?: return invalidRuleNotification()

                    ruleBase.id.stringValue ?: return invalidRuleNotification()
                }

                else -> return invalidRuleNotification()
            }

        val toolWindow =
            ToolWindowManager.getInstance(project).getToolWindow(GraphToolWindowFactory.TOOL_WINDOW_ID) ?: return

        val content = toolWindow.contentManager.getContent(0) ?: return
        val component = content.component as? GraphToolWindowContent ?: return

        val configurationService = project.service<JqaConfigurationService>()
        val ruleSet = configurationService.getAvailableRules()

        val currentRule =
            ruleSet.findRuleById(ruleId) ?: return project.notifyBalloon(
                MessageBundle.message("graph.rule.not.found", ruleId),
                NotificationType.ERROR,
            )

        component.refreshGraph(currentRule, ruleSet)
    }
}
