package org.jqassistant.tooling.intellij.plugin.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * The service is intended to be used instead of a project/application as a parent disposable.
 * Copied from
 * [intellij-community/python/openapi/src/com/jetbrains/python/PythonPluginDisposable.java](https://github.com/JetBrains/intellij-community/blob/d232a78a114ab5150c25bc5726fd909cbb0b374d/python/openapi/src/com/jetbrains/python/PythonPluginDisposable.java)
 *
 * See [https://plugins.jetbrains.com/docs/intellij/disposers.html#choosing-a-disposable-parent](https://plugins.jetbrains.com/docs/intellij/disposers.html#choosing-a-disposable-parent)
 * for more information
 */
@Service(Service.Level.APP, Service.Level.PROJECT)
class JQAssistantPluginDisposable : Disposable {
    override fun dispose() {
    }

    companion object {
        val instance: Disposable
            get() =
                ApplicationManager.getApplication().service<JQAssistantPluginDisposable>()

        fun getInstance(project: Project): Disposable = project.service<JQAssistantPluginDisposable>()
    }
}
