package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class VirtualFileExtractor : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

        val injectedVF = getInjectedVirtualFile(psiFile) ?: return
        println(injectedVF.readText())
        println(injectedVF.fileType)
        println(ConfigFileUtils.isJqaConfigFile(injectedVF, project))
    }

    private fun findYamlTag(tag: XmlTag): XmlTag? {
        if (tag.name == "yaml") {
            return tag
        }
        // Recursively search in child tags
        for (subTag in tag.subTags) {
            val found = findYamlTag(subTag)
            if (found != null) {
                return found
            }
        }
        return null
    }

    fun getInjectedVirtualFile(psiFile: PsiFile): VirtualFile? {
        if (psiFile.name == "pom.xml") {
            if (psiFile is XmlFile) {
                val rootTag = psiFile.rootTag ?: return null
                val yamlTag = findYamlTag(rootTag) ?: return null
                val manager = InjectedLanguageManager.getInstance(psiFile.project)
                val yamlContent =
                    yamlTag.value.textElements
                        .firstOrNull { it is PsiLanguageInjectionHost && it.isValidHost } as? PsiLanguageInjectionHost
                        ?: return null

                val injectedPsi = manager.findInjectedElementAt(psiFile, yamlContent.textOffset)

                // Überprüfen, ob ein injiziertes PSI existiert
                if (injectedPsi != null) {
                    val injectedFile = injectedPsi.containingFile
                    val virtualFile = injectedFile.virtualFile
                    return virtualFile
                }
            }
        }

        return null
    }
}

class YamlConfigReader : PsiElementVisitor() {
    override fun visitElement(element: com.intellij.psi.PsiElement) {
        if (element is XmlTag && element.name == "yaml") {
            val yamlContent = element.value.text
            // Verarbeiten Sie den YAML-Inhalt
            println("Gefundener YAML-Inhalt: $yamlContent")
        }
        super.visitElement(element)
    }
}
