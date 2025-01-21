package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlTag
import org.jetbrains.yaml.YAMLLanguage

/**
 * Injects a chosen language into the given context
 */
class YamlPomXmlInjector : MultiHostInjector {
    override fun getLanguagesToInject(
        registrar: MultiHostRegistrar,
        context: PsiElement,
    ) {
        // Check if the context element is an `XmlTag` and has the desired structure
        if (context is XmlTag && isConfigPlace(context)) {
            // Check if we can find the `yaml` tag
            val yamlXmlTag = findYamlTag(context) ?: return

            // Get the text content that can be used as the injection host
            val yamlContent =
                yamlXmlTag.value.textElements
                    .firstOrNull { it is PsiLanguageInjectionHost && it.isValidHost } as? PsiLanguageInjectionHost
                    ?: return

            // Calculate the correct TextRange within the host
            val yamlContentRange = TextRange(0, yamlContent.textLength)

            // Inject the YAML language
            registrar.startInjecting(YAMLLanguage.INSTANCE)
            registrar.addPlace(
                null, // No prefix
                null, // No suffix
                yamlContent, // The injection host
                yamlContentRange, // The range within the host to inject into
            )
            registrar.doneInjecting()
        }
    }

    /**
     * Gets a List of every XmlTag that could be injected.
     */
    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(XmlTag::class.java)

    /**
     * Checks if the given context is part of the `pom.xml` file.
     */
    fun isConfigPlace(context: PsiElement): Boolean {
        val psiFile = context.containingFile ?: return false
        return psiFile.name == "pom.xml"
    }

    /**
     * Searches for a child tag with the name `yaml` recursively.
     */
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
}
