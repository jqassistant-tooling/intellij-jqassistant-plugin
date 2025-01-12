package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.yaml.YAMLLanguage

class YamlXmlInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        println("NotDoneInjecting1")
        if (isConfigPlace(context)) {
            println("NotDoneInjecting2")
            val file = context.containingFile as? XmlFile ?: return
            val rootTag = file.rootTag ?: return
            val yamlXmlTag = findYamlTag(rootTag)
            if (yamlXmlTag != null) {
                val yamlContent =
                    yamlXmlTag.value.textElements
                        .firstOrNull { it is PsiLanguageInjectionHost }
                        ?.takeIf { it is PsiLanguageInjectionHost && it.isValidHost } as? PsiLanguageInjectionHost
                        ?: return
                println(yamlContent.text)
                val yamlContentRange = yamlContent.textRange as TextRange

                registrar
                    .startInjecting(YAMLLanguage.INSTANCE)
                    .addPlace(
                        "<yaml>",
                        "</yaml>",
                        yamlContent,
                        yamlContentRange,
                    ).doneInjecting()
                println("DoneInjecting")
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement?>?> = listOf(XmlTag::class.java)

    fun isConfigPlace(context: PsiElement): Boolean {
        val psiFile = context.containingFile ?: return false
        return psiFile.name == "pom.xml"
    }

    private fun findYamlTag(tag: XmlTag): XmlTag? {
        if (tag.name == "yaml") {
            return tag
        }
        // Durchsuche die Kinder-Tags
        for (subTag in tag.subTags) {
            val found = findYamlTag(subTag)
            if (found != null) {
                return found
            }
        }
        return null
    }
}
