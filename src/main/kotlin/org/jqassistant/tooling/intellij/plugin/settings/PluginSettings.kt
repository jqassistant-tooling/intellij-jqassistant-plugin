// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/*
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */

enum class JqaDistribution {
    CLI,
    MAVEN,
}

@Service(Service.Level.PROJECT)
@State(name = "org.intellij.sdk.settings.JqaPluginSettings", storages = [Storage("jqaPluginSettings.xml")])
internal class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    internal class State : BaseState() {
        var distribution by enum(JqaDistribution.CLI)
        var cliExecRootDir by string()
        var cliParams by string()
        var mavenProjectFile by string()
        var mavenAdditionalProps by string()
        var mavenProjectDescription by string()
        var mavenScriptSourceDir by string()
        var mavenOutputEncoding by string()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): PluginSettings = project.service<PluginSettings>()

        const val DISPLAY_NAME = "jQAssistant Tooling"
    }
}
