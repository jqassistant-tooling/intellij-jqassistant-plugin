package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorIntegerDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.common.toMutableMap

/**
 * Indexes all rules, and associates their offset in the file to their name.
 */
class NameIndex : FileBasedIndexExtension<String, Int>() {
    object Util {
        val NAME = ID.create<String, Int>("jqassistant.rules.xml.NameIndex")
        const val VERSION = 1
    }

    override fun getName(): ID<String, Int> = Util.NAME

    override fun getIndexer(): DataIndexer<String, Int, FileContent> = object : DataIndexer<String, Int, FileContent> {
        override fun map(content: FileContent): MutableMap<String, Int> {
            // TODO: Reject files based on the effective configuration.
            val psiFile = content.psiFile as? XmlFile ?: return mutableMapOf()
            val domManager = DomManager.getDomManager(psiFile.project)
            val dom = domManager.getFileElement(psiFile, JqassistantRules::class.java) ?: return mutableMapOf()
            val root = dom.rootElement

            return listOf(root.concepts, root.constraints, root.groups).flatMap { ruleSet ->
                ruleSet.mapNotNull {
                    val name = it.id.value ?: return@mapNotNull null
                    val offset = it.xmlTag?.textOffset ?: return@mapNotNull null
                    name to offset
                }
            }.toMutableMap()
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Int> = EnumeratorIntegerDescriptor.INSTANCE

    override fun getVersion(): Int = Util.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE)

    override fun dependsOnFileContent(): Boolean = true
}
