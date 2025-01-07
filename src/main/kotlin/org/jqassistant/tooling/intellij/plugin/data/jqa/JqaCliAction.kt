package org.jqassistant.tooling.intellij.plugin.data.jqa

import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.jqassistant.tooling.intellij.plugin.data.jqa.tasks.EffectiveRulesTask

class JqaCliAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val project = event.project
            object : Task.Backgroundable(project, "Running JQA Task", true) {
                override fun run(indicator: ProgressIndicator) {
                    if (project == null) {
                        return
                    }
                    val currentThread = Thread.currentThread()
                    val originalClassLoader = currentThread.contextClassLoader
                    val pluginClassLoader = javaClass.classLoader
                    try {
                        currentThread.contextClassLoader = pluginClassLoader
                        val testResult: RuleSet? = JqaCliTaskRunner(project).runTask(EffectiveRulesTask::class)
                        println(testResult)
                    } finally {
                        currentThread.contextClassLoader = originalClassLoader
                    }
                }
            }.queue()
        }
    }
}
