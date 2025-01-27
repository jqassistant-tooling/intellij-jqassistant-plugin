package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.CompositeLanguage
import com.intellij.lang.LanguageFilter
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLLanguage

class YamlJqaConfigLanguage private constructor() : CompositeLanguage(YAMLLanguage.INSTANCE, ID) {
    companion object {
        const val ID = "YamlJqaConfigLanguage"
        val INSTANCE = YamlJqaConfigLanguage()
    }

    init {
        registerLanguageExtension(YamlJqaConfigLanguageFilter())
    }
}

class YamlJqaConfigLanguageFilter : LanguageFilter {
    override fun isRelevantForFile(psiFile: PsiFile): Boolean {
        // Wendet Ihre Sprache nur auf YAML-Dateien an
        return psiFile.fileType is XmlFileType
    }

    override fun getLanguage(): CompositeLanguage = YamlJqaConfigLanguage.INSTANCE
}
