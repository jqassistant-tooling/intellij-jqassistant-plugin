package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IntCollectionDataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.xml.DomManager

/**
 * Indexes all rules, and associates their offset in the file to their name.
 */
class NameIndex : FileBasedIndexExtension<String, Collection<Int>>() {
    object Util {
        val NAME = ID.create<String, Collection<Int>>("jqassistant.rules.xml.NameIndex")
        const val VERSION = 3
    }

    override fun getName(): ID<String, Collection<Int>> = Util.NAME

    override fun getIndexer(): DataIndexer<String, Collection<Int>, FileContent> =
        object : DataIndexer<String, Collection<Int>, FileContent> {
            override fun map(content: FileContent): MutableMap<String, List<Int>> {
                // TODO: Reject files based on the effective configuration.
                val psiFile = content.psiFile as? XmlFile ?: return mutableMapOf()
                val domManager = DomManager.getDomManager(psiFile.project)
                val dom = domManager.getFileElement(psiFile, JqassistantRules::class.java) ?: return mutableMapOf()
                val root = dom.rootElement

                val res = mutableMapOf<String, List<Int>>()
                (root.concepts + root.constraints + root.groups).forEach { rule ->
                    val name = rule.id.value ?: return@forEach
                    val offset = rule.id.xmlAttributeValue?.textOffset ?: return@forEach

                    val previousOffsets = res[name] ?: emptyList()
                    res[name] = previousOffsets + offset
                }
                return res
            }
        }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Collection<Int>> = IntCollectionDataExternalizer()

    override fun getVersion(): Int = Util.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE)

    override fun dependsOnFileContent(): Boolean = true
}
