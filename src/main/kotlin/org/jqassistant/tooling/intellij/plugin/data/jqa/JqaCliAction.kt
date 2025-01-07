package org.jqassistant.tooling.intellij.plugin.data.jqa

import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import org.jqassistant.tooling.intellij.plugin.data.jqa.tasks.EffectiveRulesTask

class JqaCliAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val currentThread = Thread.currentThread()
            val originalClassLoader = currentThread.contextClassLoader
            val pluginClassLoader = javaClass.classLoader
            try {
                currentThread.contextClassLoader = pluginClassLoader
                val testResult: RuleSet? = JqaCliTaskRunner(project).runTask(EffectiveRulesTask::class)
                // Handle the testResult as needed
            } finally {
                currentThread.contextClassLoader = originalClassLoader
            }
        }
    }
}
