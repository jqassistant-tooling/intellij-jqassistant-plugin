// Based on Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
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
    private val baseFile = LocalFileSystem.getInstance().findFileByPath(project.basePath ?: "")

    // Labels
    private val labelBasePath = JLabel("Base path: ${baseFile?.path}/").apply { isEnabled = false }
    private val labelCliExecRootDir = JLabel("Cli execution root:")
    private val labelCliParams = JLabel("Additional Cli parameters:")
    private val labelMavenWarning = JLabel()
    private val labelMavenProjectFile = JLabel("Maven project file (f.e. pom.xml):")
    private val labelMavenAdditionalProps = JLabel("Additional Maven parameters:")

    // Cli components
    private val radioBtnCli = JRadioButton("Use CLI Distribution")
    private val cliExecRootDir =
        MyTextFieldWithBrowseButton().apply {
            isEditable = false
            val descriptor =
                FileChooserDescriptorFactory.createSingleFileDescriptor().withRoots(baseFile).withTreeRootVisible(true)
            addActionListener {
                FileChooser.chooseFile(descriptor, project, baseFile) {
                    text = toRelativePath(it)
                }
            }
        }
    private val cliParams = JBTextField()

    // Maven components
    private val radioBtnMaven = JRadioButton("Use Maven Distribution")
    private val mavenProjectFile =
        MyTextFieldWithBrowseButton().apply {
            isEditable = false
            val descriptor =
                FileChooserDescriptorFactory.createSingleFileDescriptor().withRoots(baseFile).withTreeRootVisible(true)
            addActionListener {
                FileChooser.chooseFile(descriptor, project, baseFile) {
                    text = toRelativePath(it)
                }
            }
        }
    private val mavenAdditionalProps = JBTextField()

    // Advanced settings
    private var mavenProjectDescription = JBTextField()
    private var mavenScriptSourceDir =
        MyTextFieldWithBrowseButton().apply {
            isEditable = false
            val descriptor =
                FileChooserDescriptorFactory.createSingleFileDescriptor().withRoots(baseFile).withTreeRootVisible(true)
            addActionListener {
                FileChooser.chooseFile(descriptor, project, baseFile) {
                    text = toRelativePath(it)
                }
            }
        }
    private var mavenOutputEncoding = JBTextField()

    init {

        mavenOrCliButtonGroup.add(radioBtnCli)
        mavenOrCliButtonGroup.add(radioBtnMaven)
        radioBtnCli.addActionListener(RadioButtonActionListener())
        radioBtnMaven.addActionListener(RadioButtonActionListener())

        // TODO maven module not available?
        labelMavenWarning.text = "Warning: Test Warning - Maven module not found"
        labelMavenWarning.foreground = JBColor.YELLOW
        labelMavenWarning.isVisible = false // false removes and acts like "gone"
        panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(labelBasePath, 1)
                .addComponent(radioBtnCli, 10)
                .setFormLeftIndent(15)
                .addLabeledComponent(labelCliExecRootDir, mavenProjectFile, 10, false)
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

    var myDistribution: JqaDistribution
        get() =
            if (radioBtnCli.isSelected) {
                JqaDistribution.CLI
            } else {
                JqaDistribution.MAVEN
            }
        set(newStatus) {
            if (newStatus == JqaDistribution.CLI) {
                radioBtnCli.isSelected = true
            } else {
                radioBtnMaven.isSelected = true
            }
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
        validateState()
    }

    fun validateState(): Boolean {
        val state = AtomicBoolean(true)
        val browsingFields = listOf(cliExecRootDir, mavenProjectFile, mavenScriptSourceDir)
        val futures =
            browsingFields.map { field ->
                ApplicationManager
                    .getApplication()
                    .executeOnPooledThread {
                        if (field.isEnabled &&
                            !field.validatePath(baseFile?.path ?: "")
                        ) {
                            showFileOrDirectoryError(field)
                            state.set(false)
                        } else {
                            hideDirectoryError(field)
                        }
                    }
            }

        // Make IntelliJ wait for tasks
        futures.forEach { it.get() }
        return state.get()
    }

    private fun toRelativePath(absolute: VirtualFile): String {
        val base = File(baseFile?.path.toString()).toPath()
        val myAbsolute = File(absolute.path).toPath()
        return base.relativize(myAbsolute).toCanonicalPath()
    }

    private fun showFileOrDirectoryError(field: MyTextFieldWithBrowseButton) {
        val isChooseFiles = field.fileChooserDescriptor?.isChooseFiles
        val isChooseFolders = field.fileChooserDescriptor?.isChooseFolders
        val message =
            if (isChooseFiles == true && isChooseFolders == true) {
                "Invalid file or directory"
            } else if (isChooseFiles == true) {
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
}
