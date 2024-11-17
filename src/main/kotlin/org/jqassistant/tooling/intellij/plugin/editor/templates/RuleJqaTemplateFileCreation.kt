package org.jqassistant.tooling.intellij.plugin.editor.templates

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File


class AddJqassistantYamlAction : AnAction("Add .jqassistant.yaml") {
    override fun actionPerformed(e: AnActionEvent) {
        // Query the selected directory
        val project = e.project ?: return
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
        val directoryPath = virtualFile.path
        val jqassistantFile = File(directoryPath, ".jqassistant.yaml")

        // Check whether the file already exists
        if (jqassistantFile.exists()) {
            Messages.showErrorDialog(
                project,
                ".jqassistant.yaml already exists in $directoryPath",
                "File Exists",
            )
            openFileAutomatic(project, jqassistantFile)
            return
        }

        // Copy resource file
        val resourceStream = javaClass.getResourceAsStream("/templates/.jqassistant.yaml")
        if (resourceStream != null) {
            jqassistantFile.outputStream().use { output ->
                resourceStream.copyTo(output)
            }
            Messages.showInfoMessage(
                project,
                ".jqassistant.yaml was successfully added to $directoryPath",
                "File Created",
            )

            // Update project structure for new files
            VirtualFileManager.getInstance().syncRefresh()
            openFileAutomatic(project, jqassistantFile)
        } else {
            Messages.showErrorDialog(
                project,
                "Resource file jqassistant.yaml not found in plugin resources.",
                "Error",
            )
        }
    }
    fun openFileAutomatic(project : Project, newFile : File) {
        val virtualFile: VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(File(newFile.path))
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } else {
            Messages.showErrorDialog(
                project,
                "The file was created but could not be opened automatically.",
                "Error Opening File"
            )
        }
    }
}

class AddCustomRulesXmlAction : AnAction("Create Custom Rules File") {
    override fun actionPerformed(e: AnActionEvent) {
        // Query the selected directory
        val project = e.project ?: return
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
        val directoryPath = virtualFile.path

        // Custom name prompt
        val customName = Messages.showInputDialog(
            project,
            "Enter the name for the Rule XML file (without .xml):",
            "Custom Rules File",
            Messages.getQuestionIcon()
        )?.trim() // trim() removes leading and trailing spaces

        // Verify that the user provided valid input
        if (customName.isNullOrEmpty()) {
            Messages.showWarningDialog(
                project,
                "The file name cannot be empty.",
                "Invalid Input",
            )
            return // Cancel if input is empty
        }
        // Check if only acceptable Characters are used for file name
        if(!isNameValid(customName)) {
            Messages.showWarningDialog(
                project,
                "The file name cannot be contaminated with abstract Characters",
                "Invalid Input",
            )
            return // Cancel if input is not supported
        }
        // File name with the user-defined name and the extension .xml
        val targetFile = File(directoryPath, "$customName.xml")

        // Check whether the file already exists
        if (targetFile.exists()) {
            Messages.showErrorDialog(
                project,
                "A file named $customName.xml already exists in $directoryPath",
                "File Exists",
            )
            openFileAutomatic(project, targetFile)
            return
        }

        // Load template from resources and insert the name
        val resourceStream = javaClass.getResourceAsStream("/templates/my_rules.xml")
        if (resourceStream != null) {
            val content = resourceStream.bufferedReader().use { it.readText() }

            // Replace placeholders in content with custom name
            val modifiedContent = content.replace("{{CUSTOM_NAME}}", customName)

            // Save file with the modified content in the target directory
            targetFile.writeText(modifiedContent)

            Messages.showInfoMessage(
                project,
                "$customName.xml was successfully added to $directoryPath",
                "File Created",
            )
            // Update project structure for new files
            VirtualFileManager.getInstance().syncRefresh()
            openFileAutomatic(project, targetFile)
        } else {
            Messages.showErrorDialog(
                project,
                "Template file my_rules.xml not found in plugin resources.",
                "Error",
            )
        }
    }

    fun isNameValid(name: String) : Boolean{
        //TODO considering better options for validation
        //All supported Characters of a file name
        val validNameRegex = Regex("^[a-zA-Z0-9._-]+$")
        // Additional check: No control characters or invalid Unicode characters
        return name.matches(validNameRegex) && name.codePoints().noneMatch { Character.isISOControl(it) }
    }

    fun openFileAutomatic(project : Project, newFile : File) {
        val virtualFile: VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(File(newFile.path))
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } else {
            Messages.showErrorDialog(
                project,
                "The file was created but could not be opened automatically.",
                "Error Opening File"
            )
        }
    }

    //Duplication is noted, will be refactored in final product anyway
}
