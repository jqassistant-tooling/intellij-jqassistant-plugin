package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.PomTargetPsiElement
import com.intellij.util.xml.DomTarget
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Concept
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Constraint
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Group
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

        component.currentRule = ruleBase
        when (ruleBase) {
            is Concept -> {
                component.concepts = ruleBase.providesConcept.toMutableList()
                component.concepts.addAll(0, ruleBase.requiresConcept)
                component.constraints = mutableListOf()
                component.groups = mutableListOf()
            }

            is Constraint -> {
                component.concepts = ruleBase.requiresConcept.toMutableList()
                component.constraints = mutableListOf()
                component.groups = mutableListOf()
            }

            is Group -> {
                component.groups = ruleBase.includeGroup.toMutableList()
                component.constraints = ruleBase.includeConstraint.toMutableList()
                component.concepts = ruleBase.includeConcept.toMutableList()
            }
        }
    }
}
