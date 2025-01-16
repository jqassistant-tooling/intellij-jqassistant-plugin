// Based on Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class PluginSettingsComponent(
    val project: Project,
) {
    val panel: JPanel

    private val mavenOrCliButtonGroup = ButtonGroup()
    private val radioBtnCli = JRadioButton("Use CLI Distribution")
    private val radioBtnMaven = JRadioButton("Use Maven Distribution")
    private val cliExecRootDir = TextFieldWithBrowseButton()
    private val cliParams = JBTextField()
    private val mavenWarning = JLabel()
    private val mavenProjectDir = TextFieldWithBrowseButton()
    private val mavenAdditionalProps = JBTextField()

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

        mavenProjectDir.addBrowseFolderListener(
            "Search Directory",
            "Select Maven project",
            project,
            FileChooserDescriptor(true, true, false, false, false, false),
        )

        mavenScriptSourceDir.addBrowseFolderListener(
            "Search Directory",
            "Select Maven script source",
            project,
            FileChooserDescriptor(false, true, false, false, false, false),
        )
        // TODO maven module not available?
        mavenWarning.text = "Warning: Test Warning - Maven module not found"
        mavenWarning.foreground = JBColor.RED
        mavenWarning.isVisible = true // false removes and acts like "gone"

        panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(radioBtnCli, 1)
                .setFormLeftIndent(15)
                .addLabeledComponent("CLI execution root:", cliExecRootDir, 10, false)
                .addLabeledComponent("Additional Cli parameters:", cliParams, 10, false)
                .setFormLeftIndent(1)
                .addComponent(radioBtnMaven, 10)
                .setFormLeftIndent(15)
                .addComponent(mavenWarning, 10)
                .addLabeledComponent("Maven project directory:", mavenProjectDir, 10, false)
                .addLabeledComponent("Additional Maven parameters:", mavenAdditionalProps, 10, false)
                .setFormLeftIndent(1)
                .addSeparator()
                .addTooltip("Advanced settings")
                .addLabeledComponent("Maven project description:", mavenProjectDescription, 10, false)
                .addLabeledComponent("Maven script source directory:", mavenScriptSourceDir, 10, false)
                .addLabeledComponent("Maven output encoding:", mavenOutputEncoding, 10, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel

        mavenOrCliButtonGroup.clearSelection()
        radioBtnMaven.isSelected = true
        updateEnabledComponents()
    }

    val preferredFocusedComponent: JComponent
        get() = mavenAdditionalProps

    var isCliSelected: Boolean
        get() = radioBtnCli.isSelected
        set(newStatus) {
            radioBtnCli.isSelected = newStatus
        }

    var myCliExecRootDir: String
        get() = cliExecRootDir.text
        set(newText) {
            cliExecRootDir.text = newText
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

    fun updateEnabledComponents() {
        cliParams.isEnabled = radioBtnCli.isSelected
        cliExecRootDir.isEnabled = radioBtnCli.isSelected
        mavenProjectDir.isEnabled = radioBtnMaven.isSelected
        mavenAdditionalProps.isEnabled = radioBtnMaven.isSelected
    }
}
