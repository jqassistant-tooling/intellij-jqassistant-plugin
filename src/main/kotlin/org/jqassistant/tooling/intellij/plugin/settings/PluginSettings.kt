// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/*
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */
@State(name = "org.intellij.sdk.settings.JqaPluginSettings", storages = [Storage("jqaPluginSettings.xml")])
internal class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    internal class State {
        var useCliDistro: Boolean = false
        var cliExecRootDir: String = ""
        var cliParams: String = ""
        var useMavenDistro: Boolean = true
        var mavenProjectFile: String = ""
        var mavenAdditionalProps: String = ""
        var mavenProjectDescription: String = ""
        var mavenScriptSourceDir: String = ""
        var mavenOutputEncoding: String = ""
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: PluginSettings
            get() = ApplicationManager.getApplication().service<PluginSettings>()
        const val DISPLAY_NAME = "jQAssistant Tooling"
    }
}
