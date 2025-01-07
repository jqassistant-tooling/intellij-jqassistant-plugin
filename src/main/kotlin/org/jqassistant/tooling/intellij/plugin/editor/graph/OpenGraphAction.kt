package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.pom.PomTargetPsiElement
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Concept
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Constraint
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Group
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class OpenGraphAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val currentElement = event.getData(CommonDataKeys.PSI_ELEMENT) ?: return

        val domElement = currentElement as? PomTargetPsiElement ?: return

        domElement.reference

        val target = domElement.target as? RuleBase ?: return

        val project = event.project ?: return
        val service = project.service<JqaRuleIndexingService>()

        when (target) {
            is Concept -> {
                val provides = target.providesConcept
                val requires = target.requiresConcept
            }

            is Constraint -> {
                val requires = target.requiresConcept
            }

            is Group -> {
                val groups = target.includeGroup
                val constraints = target.includeConstraint
                val concepts = target.includeConcept

                println(groups)
                println(constraints)
                println(concepts)
            }
        }

        println(domElement)
    }
}
