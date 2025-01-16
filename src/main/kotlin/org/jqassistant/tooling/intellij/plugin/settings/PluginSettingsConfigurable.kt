package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.options.Configurable
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
            Objects.requireNonNull(PluginSettings.instance.getState())
        return mySettingsComponent?.myCliExecRootDir != state.jqaHome ||
            mySettingsComponent?.isCliSelected != state.prefetchEffectiveConfig
    }

    override fun apply() {
        val state: PluginSettings.State =
            Objects.requireNonNull(PluginSettings.instance.getState())
        state.jqaHome = mySettingsComponent!!.myCliExecRootDir
        state.prefetchEffectiveConfig = mySettingsComponent!!.isCliSelected
    }

    override fun reset() {
        val state: PluginSettings.State =
            Objects.requireNonNull(PluginSettings.instance.getState())
        mySettingsComponent?.myCliExecRootDir = state.jqaHome
        mySettingsComponent?.isCliSelected = state.prefetchEffectiveConfig
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
