package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.yaml.YAMLLanguage

class YamlXmlInjector : LanguageInjectionContributor {
    override fun getInjection(context: PsiElement): Injection? {
        val file = context.containingFile
        if (file.name != "pom.xml") return null

        // Rekursive Suche nach dem <yaml>-Tag
        val xmlTag = findYamlTag(file)
        if (xmlTag != null) {
            // Bereich des <yaml>-Tags (d.h. vom öffnenden bis zum schließenden Tag)
            val textRange = xmlTag.textRange
            // Die YAML-Sprache für die Injektion festlegen
            val yamlLanguage = YAMLLanguage.INSTANCE
            return SimpleInjection(yamlLanguage, "<yaml>", "</yaml>", null)
        }
        return null
    }

    private fun findYamlTag(file: PsiFile): XmlTag? {
        // Rekursive Suche nach dem <yaml>-Tag
        return findYamlTagRecursive(file, file.firstChild)
    }

    private fun findYamlTagRecursive(
        file: PsiFile,
        element: PsiElement?,
    ): XmlTag? {
        if (element == null) return null

        // Wenn das Element ein XML-Tag ist und den Namen "yaml" hat, zurückgeben
        if (element is XmlTag && element.name == "yaml") {
            return element
        }

        // Rekursiv in den Kind-Elementen nach dem <yaml>-Tag suchen
        for (child in element.children) {
            val result = findYamlTagRecursive(file, child)
            if (result != null) return result
        }

        return null
    }
}
