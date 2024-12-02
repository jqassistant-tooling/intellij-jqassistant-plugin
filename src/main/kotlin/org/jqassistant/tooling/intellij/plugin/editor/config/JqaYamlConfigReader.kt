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
    var jqaConfigContent =
        "jqassistant:\n" +
            "  plugins:\n" +
            "    - group-id: {{CUSTOM_GROUPID}}\n" +
            "      artifact-id: {{CUSTOM_ARTIFACTID}}\n" +
            "      version: {{CUSTOM_VERSION}}\n" +
            "  scan:\n" +
            "\n" +
            "    include:\n" +
            "      files:\n" +
            "  analyze:\n" +
            "\n" +
            "    groups:\n" +
            "      {{CUSTOM_GROUPNAME}}"

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
                val versionTag = pluginTag.findFirstSubTag("version") ?: continue
                val configurationTag = pluginTag.findFirstSubTag("configuration") ?: continue
                val yamlTag = configurationTag.findFirstSubTag("yaml") ?: continue

                jqaConfigContent =
                    jqaConfigContent
                        .replace("{{CUSTOM_GROUPID}}", groupIdTag.value.trimmedText)
                        .replace("{{CUSTOM_ARTIFACTID}}", artifactIdTag.value.trimmedText)
                        .replace("{{CUSTOM_VERSION}}", versionTag.value.trimmedText)

                val yamlContent = yamlTag.text?.trim() ?: ""
                val customGroupName = getCustomGroupName(yamlContent)
                if (customGroupName != null) {
                    jqaConfigContent = jqaConfigContent.replace("{{CUSTOM_GROUPNAME}}", customGroupName)
                }

                // Create the LightVirtualFile with the YAML content

                return LightVirtualFile("jqassistant-config.yaml", jqaConfigContent)
            }
        }
        return null
    }

    fun getCustomGroupName(input: String): String? {
        val groupsSection = input.substringAfter("groups:").trim()

        // Schritt 2: Zeilen einzeln betrachten und die Zeile mit dem Bindestrich finden
        val lines = groupsSection.lines()
        val desiredLine = lines.firstOrNull { it.trim().startsWith("-") }?.trim()
        return desiredLine
    }

    fun openYamlInEditor(project: Project, yamlContent: String) {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("yaml")
        val yamlFile = LightVirtualFile("extracted.yaml", fileType, yamlContent)
        FileEditorManager.getInstance(project).openFile(yamlFile, true)
    }
}
