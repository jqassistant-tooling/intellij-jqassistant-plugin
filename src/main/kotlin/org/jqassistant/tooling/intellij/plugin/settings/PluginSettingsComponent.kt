// Based on Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jqassistant.tooling.intellij.plugin.common.FileValidator
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class PluginSettingsComponent(
    private val project: Project,
) {
    // Misc
    private val mavenOrCliButtonGroup = ButtonGroup()
    val panel: JPanel

    // Labels
    private val labelCliExecRootDir = JLabel("Cli execution root:")
    private val labelCliParams = JLabel("Additional Cli parameters:")
    private val labelMavenWarning = JLabel()
    private val labelMavenProjectFile = JLabel("Maven project file (f.e. pom.xml):")
    private val labelMavenAdditionalProps = JLabel("Additional Maven parameters:")

    // Cli components
    private val radioBtnCli = JRadioButton("Use CLI Distribution")
    private val cliExecRootDir = TextFieldWithBrowseButton()
    private val cliParams = JBTextField()

    // Maven components
    private val radioBtnMaven = JRadioButton("Use Maven Distribution")
    private val mavenProjectFile = TextFieldWithBrowseButton()
    private val mavenAdditionalProps = JBTextField()

    // Advanced settings
    private var mavenProjectDescription = JBTextField()
    private var mavenScriptSourceDir = TextFieldWithBrowseButton()
    private var mavenOutputEncoding = JBTextField()

    init {
        mavenOrCliButtonGroup.add(radioBtnCli)
        mavenOrCliButtonGroup.add(radioBtnMaven)
        radioBtnCli.addActionListener(RadioButtonActionListener())
        radioBtnMaven.addActionListener(RadioButtonActionListener())

        cliExecRootDir.addBrowseFolderListener(
            "Search Directory",
            "Select CLI execution root",
            project,
            FileChooserDescriptor(true, true, false, false, false, false),
        )

        mavenProjectFile.addBrowseFolderListener(
            "Search Directory",
            "Select Maven project",
            project,
            FileChooserDescriptor(true, false, false, false, false, false),
        )

        mavenScriptSourceDir.addBrowseFolderListener(
            "Search Directory",
            "Select Maven script source",
            project,
            FileChooserDescriptor(false, true, false, false, false, false),
        )

        // TODO maven module not available?
        labelMavenWarning.text = "Warning: Test Warning - Maven module not found"
        labelMavenWarning.foreground = JBColor.YELLOW
        labelMavenWarning.isVisible = true // false removes and acts like "gone"

        panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(radioBtnCli, 1)
                .setFormLeftIndent(15)
                .addLabeledComponent(labelCliExecRootDir, cliExecRootDir, 10, false)
                .addLabeledComponent(labelCliParams, cliParams, 10, false)
                .setFormLeftIndent(1)
                .addComponent(radioBtnMaven, 10)
                .setFormLeftIndent(15)
                .addComponent(labelMavenWarning, 10)
                .addLabeledComponent(labelMavenProjectFile, mavenProjectFile, 10, false)
                .addLabeledComponent(labelMavenAdditionalProps, mavenAdditionalProps, 10, false)
                .setFormLeftIndent(1)
                .addSeparator()
                .addTooltip("Advanced settings")
                .addLabeledComponent("Maven project description:", mavenProjectDescription, 10, false)
                .addLabeledComponent("Maven script source directory:", mavenScriptSourceDir, 10, false)
                .addLabeledComponent("Maven output encoding:", mavenOutputEncoding, 10, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    val preferredFocusedComponent: JComponent
        get() = mavenAdditionalProps

    var isCliSelected: Boolean
        get() = radioBtnCli.isSelected
        set(newStatus) {
            radioBtnCli.isSelected = newStatus
            updateEnabledComponents()
        }

    var myCliExecRootDir: String
        get() = cliExecRootDir.text
        set(newText) {
            cliExecRootDir.text = newText
            validateState()
        }

    var myCliParams: String
        get() = cliParams.text
        set(newStatus) {
            cliParams.text = newStatus
        }

    var isMavenSelected: Boolean
        get() = radioBtnMaven.isSelected
        set(newStatus) {
            radioBtnMaven.isSelected = newStatus
            updateEnabledComponents()
        }

    var myMavenProjectFile: String
        get() = mavenProjectFile.text
        set(newText) {
            mavenProjectFile.text = newText
            validateState()
        }

    var myAdditionalMavenProperties: String
        get() = mavenAdditionalProps.text
        set(newStatus) {
            mavenAdditionalProps.text = newStatus
        }

    var myMavenProjectDescription: String
        get() = mavenProjectDescription.text
        set(newText) {
            mavenProjectDescription.text = newText
        }

    var myMavenScriptSourceDir: String
        get() = mavenScriptSourceDir.text
        set(newText) {
            mavenScriptSourceDir.text = newText
            validateState()
        }

    var myMavenOutputEncoding: String
        get() = mavenOutputEncoding.text
        set(newText) {
            mavenOutputEncoding.text = newText
        }

    private inner class RadioButtonActionListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            updateEnabledComponents()
        }
    }

    private fun updateEnabledComponents() {
        val useCli = radioBtnCli.isSelected
        val useMaven = radioBtnMaven.isSelected
        cliParams.isEnabled = useCli
        cliExecRootDir.isEnabled = useCli
        labelCliExecRootDir.isEnabled = useCli
        labelCliParams.isEnabled = useCli
        mavenProjectFile.isEnabled = useMaven
        mavenAdditionalProps.isEnabled = useMaven
        labelMavenWarning.isEnabled = useMaven
        labelMavenProjectFile.isEnabled = useMaven
        labelMavenAdditionalProps.isEnabled = useMaven
    }

    fun validateState(): Boolean {
        val state = AtomicBoolean(true)
        val cliSelected = isCliSelected
        val mavenSelected = isMavenSelected
        if (cliSelected && mavenSelected) {
            state.set(false)
        }

        val browsingFields = listOf(cliExecRootDir, mavenProjectFile, mavenScriptSourceDir)
        val futures =
            browsingFields.map { field ->
                ApplicationManager
                    .getApplication()
                    .executeOnPooledThread {
                        // TODO check for file OR Folder (not both)
                        if (field.isEnabled && !FileValidator.validateFileOrDirectory(field.text)) {
                            showFileOrDirectoryError(field)
                            state.set(false)
                        } else {
                            hideDirectoryError(field)
                        }
                    }
            }
        // Wait for tasks
        futures.forEach { it.get() }
        return state.get()
    }

    private fun showFileOrDirectoryError(field: TextFieldWithBrowseButton) {
        // TODO show error message
        val font = field.font
        field.border =
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.RED),
                "Invalid file or directory",
                0,
                0,
                font,
                JBColor.RED,
            )
    }

    private fun hideDirectoryError(field: TextFieldWithBrowseButton) {
        field.border = BorderFactory.createEmptyBorder()
    }
}
