package org.jqassistant.tooling.intellij.plugin.editor.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

class JqaBundledDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> = arrayOf("jqassistant.dic")
}
