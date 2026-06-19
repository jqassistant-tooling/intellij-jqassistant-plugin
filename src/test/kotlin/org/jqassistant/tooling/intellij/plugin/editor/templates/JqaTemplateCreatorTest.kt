package org.jqassistant.tooling.intellij.plugin.editor.templates

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import java.io.File

// Test class for verifying the functionality of actions that create jqassistant configuration files
class JqaTemplateCreatorTest : HeavyPlatformTestCase() {
    /**
     *  Test for the action that adds a .jqassistant.yaml file to a given directory
     *  This Test will only work if line 60 in RuleJqaTemplateFileCreation.kt is commented out
     *  This is due to the expected user input of the message function
     */
    fun testAddJqassistantYamlAction() {
        // Create the action and a temporary directory as a VirtualFile
        val action = AddJqassistantYamlAction()
        val virtualFile = createTempDirVirtualFile()
        val event = createActionEvent(virtualFile)

        // Perform the action
        action.actionPerformed(event)

        // Verify the .jqassistant.yaml file was created and is not empty
        val createdFile = File(virtualFile.path, ".jqassistant.yaml")
        assertTrue("The .jqassistant.yaml file was not created!", createdFile.exists())
        assertTrue("The .jqassistant.yaml file content is empty!", createdFile.readText().isNotEmpty())
    }

    /**
     *  Test for the action that adds a custom-rule.xml file with user input
     *  This Test will only work if line 60 in RuleJqaTemplateFileCreation.kt is commented out
     *  This is due to the expected user input of the message function
     */
    fun testAddCustomRulesXmlAction() {
        // Create a subclass of AddCustomRulesXmlAction to simulate user input
        val action =
            object : AddCustomRulesXmlAction() {
                // Override the method to simulate user input with "custom-rule"
                override fun getUserInput(
                    project: com.intellij.openapi.project.Project,
                    message: String,
                    initialValue: String?,
                ): String? = "custom-rule"
            }
        val virtualFile = createTempDirVirtualFile()
        val event = createActionEvent(virtualFile)

        // Perform the action
        action.actionPerformed(event)

        // Verify the custom-rule.xml file was created and contains the expected placeholder replacement
        val createdFile = File(virtualFile.path, "custom-rule.xml")
        assertTrue("The custom-rule.xml file was not created!", createdFile.exists())
        assertTrue(
            "The placeholder in the template was not replaced with 'custom-rule'.",
            createdFile.readText().contains("custom-rule"),
        )
    }

    /**
     * Creates a temporary directory as a VirtualFile.
     * Used as the target location for testing file creation.
     */
    private fun createTempDirVirtualFile(): VirtualFile {
        val tempDir = createTempDirectory().toPath().toFile()
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)!!
    }

    /**
     * Creates a simulated AnActionEvent with the given VirtualFile.
     * This is used to simulate the context in which an action is performed.
     */
    private fun createActionEvent(virtualFile: VirtualFile): AnActionEvent {
        val dataContext =
            DataContext { key ->
                when (key) {
                    // Provide the project and virtual file as context data
                    CommonDataKeys.PROJECT.name -> project
                    CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                    else -> null
                }
            }

        // Create a new Presentation instance to represent the action's visual and execution state
        val presentation = Presentation()

        // Return an AnActionEvent with the simulated data and action manager
        return AnActionEvent(
            null,
            dataContext,
            "test",
            presentation,
            ActionManager.getInstance(),
            0,
        )
    }
}
