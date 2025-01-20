package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import java.util.Objects
import javax.swing.JComponent

/**
 * Provides controller functionality for application settings.
 */
internal class PluginSettingsConfigurable(
    val project: Project,
) : Configurable {
    private var mySettingsComponent: PluginSettingsComponent? = null

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable
    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = PluginSettings.DISPLAY_NAME

    override fun getPreferredFocusedComponent(): JComponent? = mySettingsComponent?.preferredFocusedComponent

    override fun createComponent(): JComponent? {
        mySettingsComponent = PluginSettingsComponent(project)
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val state: PluginSettings.State =
            Objects.requireNonNull(PluginSettings.getInstance(project).getState())
        var isModified = false
        isModified = isModified || mySettingsComponent?.myDistribution != state.distribution
        isModified = isModified || mySettingsComponent?.myCliExecRootDir != state.cliExecRootDir
        isModified = isModified || mySettingsComponent?.myCliParams != state.cliParams
        isModified = isModified || mySettingsComponent?.myMavenProjectFile != state.mavenProjectFile
        isModified = isModified || mySettingsComponent?.myAdditionalMavenProperties != state.mavenAdditionalProps
        isModified = isModified || mySettingsComponent?.myMavenProjectDescription != state.mavenProjectDescription
        isModified = isModified || mySettingsComponent?.myMavenScriptSourceDir != state.mavenScriptSourceDir
        isModified = isModified || mySettingsComponent?.myMavenOutputEncoding != state.mavenOutputEncoding
        if (isModified) mySettingsComponent?.validateState()
        return isModified
    }

    override fun apply() {
        val state: PluginSettings.State =
            Objects.requireNonNull(PluginSettings.getInstance(project).getState())
        if (mySettingsComponent == null) {
            throw ConfigurationException("Couldn't apply settings")
        }
        // TODO add checks for files
        if (!mySettingsComponent?.validateState()!!) {
            throw ConfigurationException("Invalid settings")
        }

        state.distribution = mySettingsComponent!!.myDistribution
        state.cliExecRootDir = mySettingsComponent!!.myCliExecRootDir
        state.cliParams = mySettingsComponent!!.myCliParams
        state.mavenProjectFile = mySettingsComponent!!.myMavenProjectFile
        state.mavenAdditionalProps = mySettingsComponent!!.myAdditionalMavenProperties
        state.mavenProjectDescription = mySettingsComponent!!.myMavenProjectDescription
        state.mavenScriptSourceDir = mySettingsComponent!!.myMavenScriptSourceDir
        state.mavenOutputEncoding = mySettingsComponent!!.myMavenOutputEncoding
    }

    override fun reset() {
        val state: PluginSettings.State =
            Objects.requireNonNull(PluginSettings.getInstance(project).getState())

        mySettingsComponent?.myDistribution = state.distribution
        mySettingsComponent?.myCliExecRootDir = state.cliExecRootDir
        mySettingsComponent?.myCliParams = state.cliParams
        mySettingsComponent?.myMavenProjectFile = state.mavenProjectFile
        mySettingsComponent?.myAdditionalMavenProperties = state.mavenAdditionalProps
        mySettingsComponent?.myMavenProjectDescription = state.mavenProjectDescription
        mySettingsComponent?.myMavenScriptSourceDir = state.mavenScriptSourceDir
        mySettingsComponent?.myMavenOutputEncoding = state.mavenOutputEncoding
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
