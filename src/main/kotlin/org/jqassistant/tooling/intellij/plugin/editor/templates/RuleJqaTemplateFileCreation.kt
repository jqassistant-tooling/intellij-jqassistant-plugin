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
 * Abstract base class for creating files from templates in a specified directory.
 * Handles user input, validates file names, and generates files based on provided templates.
 */
abstract class RuleJqaTemplateFileCreator(
    // Default file name or title to display in the action.
    private val fileName: String,
    // Path to the template resource file.
    private val templatePath: String,
    // Optional message prompting the user for a custom file name.
    private val promptMessage: String? = null,
    // Optional placeholder in the template that will be replaced with the file name.
    private val placeholder: String? = null,
) : AnAction(fileName) {
    // Entry point for the action when triggered by the user.
    override fun actionPerformed(e: AnActionEvent) {
        // Retrieve the project context or exit if not available.
        val project = e.project ?: return
        // Get the selected virtual file or directory.
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
        // Determine the directory path, adjusting if a file is selected.
        val directoryPath = if (virtualFile.isFile) virtualFile.parent.path else virtualFile.path

        // Get the target file name from user input or use the default.
        val targetName =
            promptMessage?.let {
                getValidFileName(project, it, "") ?: return // Exit if input is null.
            } ?: fileName

        // Check if the target file already exists and create it if not.
        val targetFile = getTargetFile(project, directoryPath, targetName) ?: return

        // Generate the file from the template and open it.
        createFileFromTemplate(project, targetFile, targetName)
    }

    // Reads the template file and creates a new file, replacing placeholders if specified.
    private fun createFileFromTemplate(project: Project, targetFile: File, targetName: String) {
        val resourceStream = javaClass.getResourceAsStream(templatePath)
        if (resourceStream != null) {
            // Read the template content and replace the placeholder with the target name.
            val content = resourceStream.bufferedReader().use { it.readText() }
            val modifiedContent = placeholder?.let { content.replace(it, targetName) } ?: content
            targetFile.writeText(modifiedContent) // Write the modified content to the file.

            // Show success message and refresh the file system to reflect the new file.
            Messages.showInfoMessage(project, "$targetName created in ${targetFile.parent}", "File Created")
            VirtualFileManager.getInstance().syncRefresh()
            openFile(project, targetFile)
        } else {
            // Display an error if the template file is not found.
            Messages.showErrorDialog(project, "Template file not found in resources.", "Error")
        }
    }

    // Opens the newly created file in the editor.
    private fun openFile(project: Project, file: File) {
        LocalFileSystem.getInstance().findFileByIoFile(file)?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        } ?: Messages.showErrorDialog(project, "File created but could not be opened.", "Error")
    }

    // Prompts the user for input and validates the file name recursively.
    private fun getValidFileName(project: Project, insertedMessage: String?, initialValue: String?): String? {
        var message = insertedMessage
        var input = getUserInput(project, message ?: "Enter file name:", initialValue) ?: return null
        message = promptMessage ?: return null // Reset so that only the most recently suggested name is displayed

        if (input.isEmpty()) {
            Messages.showWarningDialog(project, "File name can't be empty.", "Invalid Input")
            return null
        } else if (!isNameValid(input)) {
            Messages.showWarningDialog(
                project,
                "$input is invalid. Maybe try: ${PathUtilRt.suggestFileName(input)}",
                "Invalid Input",
            )
            input = getValidFileName(
                project,
                "$message \nSuggested name: ${PathUtilRt.suggestFileName(input)}",
                input,
            ) ?: return null // Recursive call for re-entry.
        }

        return input
    }

    // Displays an input dialog and returns the user's input.
    open fun getUserInput(project: Project, message: String, initialValue: String?): String? =
        Messages
            .showInputDialog(
                project,
                message,
                "Input",
                Messages.getQuestionIcon(),
                // Pre-fills the input field with the previous value.
                initialValue,
                null,
            )?.trim()

    // Validates the file name to ensure it adheres to naming conventions.
    private fun isNameValid(name: String): Boolean {
        if (name.endsWith(".xml")) {
            return false // Disallow file names ending with ".xml".
        } else {
            return PathUtilRt.isValidFileName("$name${getFileExtension()}", true)
        }
    }

    // Checks if the target file already exists and returns the file object if valid.
    private fun getTargetFile(project: Project, directoryPath: String, targetName: String): File? {
        val targetFile = File(directoryPath, "$targetName${getFileExtension()}")

        if (targetFile.exists()) {
            Messages.showErrorDialog(project, "File already exists in $directoryPath", "File Exists")
            openFile(project, targetFile)
            return null
        } else {
            return targetFile
        }
    }

    // Determines the file extension based on the default file name.
    private fun getFileExtension(): String = if (fileName.endsWith(".yaml")) "" else ".xml"
}

/**
 * Action to create a `.jqassistant.yaml` file using a predefined template.
 */
class AddJqassistantYamlAction :
    RuleJqaTemplateFileCreator(
        fileName = ".jqassistant.yaml",
        templatePath = "/templates/.jqassistant.yaml",
    )

/**
 * Action to create a custom XML rules file, prompting the user for a name.
 */
open class AddCustomRulesXmlAction :
    RuleJqaTemplateFileCreator(
        fileName = "Custom Rules File",
        templatePath = "/templates/my_rules.xml",
        promptMessage = "Enter the name for the Rule XML file (without .xml):",
        // Placeholder in the template to replace.
        placeholder = "{{CUSTOM_NAME}}",
    )
