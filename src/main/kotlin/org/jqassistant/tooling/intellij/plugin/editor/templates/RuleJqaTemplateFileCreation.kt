package org.jqassistant.tooling.intellij.plugin.editor.templates

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.util.PathUtilRt
import java.io.File

/**
 *   Abstract base class for creating files from templates in a specified directory.
 *   The class handles user input, validates file names, and generates files based on templates.
 **/
abstract class RuleJqaTemplateFileCreator(
    // Default file name or prompt title.
    private val fileName: String,
    // Path to the template resource file.
    private val templatePath: String,
    // Optional user prompt message for custom file names.
    private val promptMessage: String? = null,
    // Optional placeholder in the template to replace.
    private val placeholder: String? = null,
) : AnAction(fileName) {
    // Entry point for the action when triggered.
    override fun actionPerformed(e: AnActionEvent) {
        // Ensure the project context is available.
        val project = e.project ?: return
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
        // Get the path of the selected directory.
        var directoryPath = virtualFile.path

        // Check if selected directory is a File
        if (virtualFile.isFile) {
            directoryPath = virtualFile.parent.path // Set directoryPath to path of selected File
        }

        // Get the target file name, either from user input or default.
        val targetName =
            promptMessage?.let {
                getUserInput(project, it) ?: return // Cancel if input is null or empty.
            } ?: fileName

        // Validate the file name and show a warning if invalid.
        if (targetName.isEmpty() || !isNameValid(targetName)) {
            Messages.showWarningDialog(project, "Invalid file name.", "Invalid Input")
            return
        }

        // Create the target file in the selected directory.
        val targetFile = File(directoryPath, "$targetName${getFileExtension()}")

        // Check if the file already exists and handle accordingly.
        if (targetFile.exists()) {
            Messages.showErrorDialog(project, "File already exists in $directoryPath", "File Exists")
            openFile(project, targetFile)
            return
        }

        // Generate the file from the specified template.
        createFileFromTemplate(project, targetFile, targetName)
    }

    // Creates a file from the template, replacing placeholders if necessary.
    private fun createFileFromTemplate(project: Project, targetFile: File, targetName: String) {
        val resourceStream = javaClass.getResourceAsStream(templatePath)
        if (resourceStream != null) {
            val content = resourceStream.bufferedReader().use { it.readText() } // Read the template content.
            val modifiedContent =
                placeholder?.let {
                    content.replace(it, targetName)
                } ?: content // Replace placeholder if provided.
            targetFile.writeText(modifiedContent) // Write the modified content to the new file.

            // Show success message and refresh the project to reflect the new file.
            Messages.showInfoMessage(project, "$targetName created in ${targetFile.parent}", "File Created")
            VirtualFileManager.getInstance().syncRefresh()
            openFile(project, targetFile)
        } else {
            // Show an error message if the template resource is not found.
            Messages.showErrorDialog(project, "Template file not found in resources.", "Error")
        }
    }

    // Opens the newly created file in the editor.
    private fun openFile(project: Project, file: File) {
        LocalFileSystem.getInstance().findFileByIoFile(file)?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        } ?: Messages.showErrorDialog(project, "File created but could not be opened.", "Error")
    }

    // Prompts the user for input using a dialog box and returns the input.
    private fun getUserInput(project: Project, message: String): String? =
        Messages.showInputDialog(project, message, "Input", Messages.getQuestionIcon())?.trim()

    // Validates the file name to ensure it contains only allowed characters and no control characters.
    private fun isNameValid(name: String): Boolean {
        if (name.endsWith(".xml")) {
            return false
        } else {
            return PathUtilRt.isValidFileName(name, true)
        }
    }

    // Determines the appropriate file extension based on the default file name.
    private fun getFileExtension(): String = if (fileName.endsWith(".yaml")) "" else ".xml"
}

/**
 * Action to create a `.jqassistant.yaml` file using a predefined template.
 * **/
class AddJqassistantYamlAction :
    RuleJqaTemplateFileCreator(
        fileName = ".jqassistant.yaml",
        templatePath = "/templates/.jqassistant.yaml",
    )

/**
 * Action to create a custom XML rules file, prompting the user for a name.
 * **/
class AddCustomRulesXmlAction :
    RuleJqaTemplateFileCreator(
        fileName = "Custom Rules File",
        templatePath = "/templates/my_rules.xml",
        promptMessage = "Enter the name for the Rule XML file (without .xml):",
        // Placeholder to replace in the template.
        placeholder = "{{CUSTOM_NAME}}",
    )
