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

class RuleJqaTemplateFileTest : HeavyPlatformTestCase() {
    fun testAddJqassistantYamlAction() {
        // Arrange
        val action = AddJqassistantYamlAction()
        val virtualFile = createTempDirVirtualFile()
        val event = createActionEvent(virtualFile)

        // Act

        action.actionPerformed(event)

        // Assert
        val createdFile = File(virtualFile.path, ".jqassistant.yaml")
        assertTrue("The .jqassistant.yaml file was not created!", createdFile.exists())
        assertTrue("The .jqassistant.yaml file content is empty!", createdFile.readText().isNotEmpty())
    }

    fun testAddCustomRulesXmlAction() {
        // Arrange
        val action =
            object : AddCustomRulesXmlAction() {
                override fun getUserInput(
                    project: com.intellij.openapi.project.Project,
                    message: String,
                    initialValue: String?,
                ): String? {
                    return "custom-rule" // Simuliere Benutzereingabe
                }
            }
        val virtualFile = createTempDirVirtualFile()
        val event = createActionEvent(virtualFile)

        action.actionPerformed(event)

        // Assert
        val createdFile = File(virtualFile.path, "custom-rule.xml")
        assertTrue("The custom-rule.xml file was not created!", createdFile.exists())
        assertTrue(
            "The placeholder in the template was not replaced with 'custom-rule'.",
            createdFile.readText().contains("custom-rule"),
        )
    }

    /**
     * Erzeugt ein temporäres Verzeichnis als VirtualFile.
     */
    private fun createTempDirVirtualFile(): VirtualFile {
        val tempDir = createTempDirectory().toPath().toFile()
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)!!
    }

    /**
     * Erstellt ein simuliertes AnActionEvent mit einem gegebenen VirtualFile.
     */
    private fun createActionEvent(virtualFile: VirtualFile): AnActionEvent {
        val dataContext =
            DataContext { key ->
                when (key) {
                    CommonDataKeys.PROJECT.name -> project
                    CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                    else -> null
                }
            }

        val presentation = Presentation() // Erstelle eine neue Presentation-Instanz

        return AnActionEvent(
            // inputEvent =
            null,
            // dataContext =
            dataContext,
            // place =
            "test",
            // presentation =
            presentation,
            // actionManager =
            ActionManager.getInstance(),
            // modifiers =
            0,
        )
    }
}
