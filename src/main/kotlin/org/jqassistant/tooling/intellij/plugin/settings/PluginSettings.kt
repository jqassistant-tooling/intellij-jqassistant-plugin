// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jqassistant.tooling.intellij.plugin.data.config.JqaDistribution

/**
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */

@Service(Service.Level.PROJECT)
@State(
    name = "jqassistant.JqaPluginSettings",
    storages = [Storage("jqaPluginSettings.xml", roamingType = RoamingType.PER_OS)],
)
internal class PluginSettings : SimplePersistentStateComponent<PluginSettings.State>(State()) {
    internal class State : BaseState() {
        /**
         * The distribution of this jQA project.
         */
        var distribution by enum(JqaDistribution.CLI)

        /**
         * Execution root for the cli distribution.
         *
         * If `null` the project root is used.
         */
        var cliExecRootDir by string()

        /**
         * Additional parameters used when the cli distribution is active.
         */
        var cliParams by string()

        /**
         * Specific maven pom of the maven project to use for the maven distribution.
         *
         * If `null` searches for a maven project with the jQA plugin.
         */
        var mavenProjectFile by string()

        /**
         * Additional parameters used when the maven distribution is active. They are parsed in the same format as
         * cli parameters, but only properties are supported.
         */
        var mavenAdditionalProps by string()

        /**
         * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
         */
        var mavenProjectDescription by string()

        /**
         * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
         */
        var mavenScriptSourceDir by string()

        /**
         * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
         *
         * Currently unused since the property which jQA searches doesn't exist in MavenProject.build.
         * This is probably based on an older Maven version. We keep it configurable anyway so that we could use it
         * if it became relevant.
         */
        var mavenOutputEncoding by string()
    }

    companion object {
        fun getInstance(project: Project): PluginSettings = project.service<PluginSettings>()

        const val DISPLAY_NAME = "jQAssistant Tooling"
    }
}
