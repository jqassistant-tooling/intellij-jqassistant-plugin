// Based on Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.config.JqaDistribution
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
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

    // TODO: Find a proper solution for project root paths.
    private val baseFile = project.guessProjectDir()

    // Labels
    private val labelBasePath = JLabel("Base path: ${baseFile?.presentableUrl}/").apply { isEnabled = false }
    private val labelMavenWarning = JLabel()
    private val labelMavenProjectFile = JLabel("Maven project file:")
    private val labelMavenAdditionalProps = JLabel("Additional Maven parameters:")
    private val labelAdvancedSettings = JLabel("Advanced Settings:")

    // CLI components
    private val radioBtnCli = JRadioButton("Use CLI Distribution")
    private val labelCliExecRootDir = JLabel("Execution root:")
    private val cliExecRootDir =
        ValidatorTextFieldWithBrowseButton(
            FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withRoots(project.guessProjectDir())
                .withTreeRootVisible(true),
        ).apply {
            setEmptyState("Use project root")
            addActionListener {
                FileChooser.chooseFile(descriptor, project, baseFile) {
                    text = toRelativePath(project.guessProjectDir()!!, it)
                }
            }
        }
    private val labelCliParams = JLabel("Additional parameters:")
    private val cliParams = JBTextField()

    // Maven components
    private val radioBtnMaven = JRadioButton("Use Maven Distribution")
    private val mavenProjectFile =
        ValidatorTextFieldWithBrowseButton(
            FileChooserDescriptorFactory
                .createSingleFileDescriptor("xml")
                .withRoots(project.guessProjectDir())
                .withTreeRootVisible(true),
        ).apply {
            setEmptyState("Use default jQA Maven Plugin")
            addActionListener {
                FileChooser.chooseFile(this.descriptor, project, baseFile) {
                    text = toRelativePath(project.guessProjectDir()!!, it)
                }
            }
        }
    private val mavenAdditionalProps = JBTextField()

    // Advanced settings
    private var mavenProjectDescription = JBTextField()
    private var mavenScriptSourceDir =
        ValidatorTextFieldWithBrowseButton(
            FileChooserDescriptorFactory
                .createSingleFolderDescriptor()
                .withRoots(project.guessProjectDir())
                .withTreeRootVisible(true),
        ).apply {
            addActionListener {
                FileChooser.chooseFile(descriptor, project, baseFile) {
                    text = toRelativePath(project.guessProjectDir()!!, it)
                }
            }
        }
    private var mavenOutputEncoding = JBTextField()

    init {
        // RadioGroup
        mavenOrCliButtonGroup.add(radioBtnCli)
        mavenOrCliButtonGroup.add(radioBtnMaven)
        radioBtnCli.addActionListener(RadioButtonActionListener())
        radioBtnMaven.addActionListener(RadioButtonActionListener())

        labelMavenWarning.text = "Warning: Maven module not found"
        labelMavenWarning.foreground = JBColor.RED
        labelMavenWarning.isVisible =
            !project
                .service<JqaConfigurationService>()
                .isMavenDistributionSupported()

        // Build Form
        panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(labelBasePath, 1)
                .addComponent(radioBtnCli, 10)
                .setFormLeftIndent(25)
                .addLabeledComponent(labelCliExecRootDir, cliExecRootDir, 10, false)
                .addTooltip("The execution root of the cli distribution")
                .addLabeledComponent(labelCliParams, cliParams, 10, false)
                .addTooltip("Additional parameters for the cli distribution")
                .setFormLeftIndent(1)
                .addComponent(radioBtnMaven, 10)
                .setFormLeftIndent(25)
                .addComponent(labelMavenWarning, 10)
                .addLabeledComponent(labelMavenProjectFile, mavenProjectFile, 10, false)
                .addTooltip("Select project with jQA Maven Plugin")
                .addLabeledComponent(labelMavenAdditionalProps, mavenAdditionalProps, 10, false)
                .addTooltip("Additional parameters for the maven distribution")
                .setFormLeftIndent(1)
                .addSeparator()
                .addComponent(labelAdvancedSettings)
                .setFormLeftIndent(25)
                .addLabeledComponent("Maven project description:", mavenProjectDescription, 10, false)
                .addTooltip("Overrides description text of the maven project usually found in pom.xml")
                .addLabeledComponent("Maven script source directory:", mavenScriptSourceDir, 10, false)
                .addTooltip("The source directory where maven expects your project files")
                .addLabeledComponent("Maven output encoding:", mavenOutputEncoding, 10, false)
                .addTooltip("The character encoding scheme used for reading and writing files")
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    val preferredFocusedComponent: JComponent
        get() = mavenAdditionalProps

    var myDistribution: JqaDistribution
        get() =
            if (radioBtnCli.isSelected) {
                JqaDistribution.CLI
            } else {
                JqaDistribution.MAVEN
            }
        set(newStatus) =
            when (newStatus) {
                JqaDistribution.CLI -> radioBtnCli.isSelected = true
                JqaDistribution.MAVEN -> radioBtnMaven.isSelected = true
            }.also { updateEnabledComponents() }

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
        validateState()
    }

    fun validateState(): Boolean {
        var state = true
        val fields = listOf(cliExecRootDir, mavenProjectFile, mavenScriptSourceDir)
        fields.forEach { field ->
            if (field.isEnabled &&
                !field.validateRelativePath(baseFile)
            ) {
                showFileOrDirectoryError(field)
                state = false
            } else {
                hideDirectoryError(field)
            }
        }

        return state
    }

    private fun toRelativePath(base: VirtualFile, child: VirtualFile): String =
        VfsUtil.findRelativePath(base, child, '/') ?: ""

    private fun showFileOrDirectoryError(field: ValidatorTextFieldWithBrowseButton) {
        val isChooseFiles = field.descriptor.isChooseFiles
        val isChooseFolders = field.descriptor.isChooseFolders
        val message =
            if (isChooseFiles && isChooseFolders) {
                "Invalid file or directory"
            } else if (isChooseFiles) {
                "Invalid file"
            } else {
                "Invalid directory"
            }

        val font = field.font
        field.border =
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(JBColor.RED),
                message,
                0,
                0,
                font,
                JBColor.RED,
            )
    }

    private fun hideDirectoryError(field: TextFieldWithBrowseButton) {
        field.border = BorderFactory.createEmptyBorder()
    }

    private inner class RadioButtonActionListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            updateEnabledComponents()
        }
    }
}
