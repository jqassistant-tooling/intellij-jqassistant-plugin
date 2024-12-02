package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.LightVirtualFile

class JqaYamlConfigReader : AnAction("Extract Jqa config from pom.xml") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return

        if (virtualFile.name != "pom.xml") {
            Messages.showMessageDialog(
                project,
                "This action only works with a pom.xml file.",
                "InvalidFile",
                Messages.getWarningIcon(),
            )
            return
        }

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return
        if (psiFile !is XmlFile) { // Make sure the file is an XmlFile here
            Messages.showErrorDialog(
                project,
                "The selected file is not a valid XML file.",
                "Fehler",
            )
            return
        }
        if (psiFile == null) {
            Messages.showErrorDialog(
                project,
                "The file could not be processed.",
                "Error",
            )
            return
        }

        // Extract YAML
        val extractedYaml = PomXmlProcessor.extractYamlFromPom(psiFile)
        if (extractedYaml != null) {
            Messages.showMessageDialog(
                project,
                "YAML successfully extracted:\n${extractedYaml.name}",
                "Success",
                Messages.getInformationIcon(),
            )
            PomXmlProcessor.openYamlInEditor(project, VfsUtil.loadText(extractedYaml))
        } else {
            Messages.showErrorDialog(
                project,
                "No YAML data found in the pom.xml.",
                "Error",
            )
        }
    }

    override fun update(e: AnActionEvent) {
        // Only activate the action if a `pom.xml` is selected
        val file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.name == "pom.xml"
    }
}

object PomXmlProcessor {
    fun extractYamlFromPom(pomFile: PsiFile): VirtualFile? {
        val xmlFile = pomFile as? XmlFile ?: return LightVirtualFile("jqassistant-config.yaml", "yamlContent0")
        val rootTag = xmlFile.rootTag ?: return LightVirtualFile("jqassistant-config.yaml", "yamlContent1")
        val buildTag =
            rootTag.findFirstSubTag("build") ?: return LightVirtualFile("jqassistant-config.yaml", "yamlContent2")
        val pluginManagementTag =
            buildTag.findFirstSubTag("pluginManagement")
                ?: return LightVirtualFile("jqassistant-config.yaml", "yamlContent3")
        val pluginsTag = pluginManagementTag?.findFirstSubTag("plugins") ?: buildTag.findFirstSubTag("plugins")
        if (pluginsTag == null) return LightVirtualFile("jqassistant-config.yaml", "yamlContent4")

        // Loop through all <plugin> tags
        for (pluginTag in pluginsTag.findSubTags("plugin")) {
            val groupIdTag = pluginTag.findFirstSubTag("groupId") ?: continue
            val artifactIdTag = pluginTag.findFirstSubTag("artifactId") ?: continue

            // Check if the plugin is the jqassistant-maven-plugin
            if (groupIdTag.value.trimmedText == "com.buschmais.jqassistant" &&
                artifactIdTag.value.trimmedText == "jqassistant-maven-plugin"
            ) {
                val configurationTag = pluginTag.findFirstSubTag("configuration") ?: continue
                val yamlTag = configurationTag.findFirstSubTag("yaml") ?: continue

                val yamlContent = yamlTag.value.trimmedText

                // Create the LightVirtualFile with the YAML content
                return LightVirtualFile("jqassistant-config.yaml", yamlContent)
            }
        }
        return null
    }

    fun openYamlInEditor(project: Project, yamlContent: String) {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("yaml")
        val yamlFile = LightVirtualFile("extracted.yaml", fileType, yamlContent)
        FileEditorManager.getInstance(project).openFile(yamlFile, true)
    }
}
