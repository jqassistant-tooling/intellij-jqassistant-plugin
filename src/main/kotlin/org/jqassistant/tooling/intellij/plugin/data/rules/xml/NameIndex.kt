package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.psi.xml.XmlFile
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumDataDescriptor
import com.intellij.util.io.EnumeratorIntegerDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType
import java.io.DataInput
import java.io.DataOutput

/**
 * Indexes all rules, and associates their type and offset in the file to their name.
 */
class NameIndex : FileBasedIndexExtension<String, Collection<NameIndex.DeclarationStub>>() {
    object Util {
        val NAME = ID.create<String, Collection<DeclarationStub>>("jqassistant.rules.xml.NameIndex")
        const val VERSION = 5
    }

    data class DeclarationStub(
        val offset: Int,
        val type: JqaRuleType,
    )

    override fun getName(): ID<String, Collection<DeclarationStub>> = Util.NAME

    override fun getIndexer(): DataIndexer<String, Collection<DeclarationStub>, FileContent> =
        object : DataIndexer<String, Collection<DeclarationStub>, FileContent> {
            override fun map(content: FileContent): MutableMap<String, List<DeclarationStub>> {
                val psiFile = content.psiFile as? XmlFile ?: return mutableMapOf()
                val domManager = DomManager.getDomManager(psiFile.project)
                val dom = domManager.getFileElement(psiFile, JqassistantRules::class.java) ?: return mutableMapOf()
                val root = dom.rootElement

                val res = mutableMapOf<String, List<DeclarationStub>>()
                (root.concepts + root.constraints + root.groups).forEach { rule ->
                    val name = rule.id.value ?: return@forEach
                    val offset = rule.id.xmlAttributeValue?.textOffset ?: return@forEach

                    val previousOffsets = res[name] ?: emptyList()
                    res[name] = previousOffsets + DeclarationStub(offset, rule.getType())
                }
                return res
            }
        }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Collection<DeclarationStub>> =
        object : DataExternalizer<Collection<DeclarationStub>> {
            val enumDescriptor = EnumDataDescriptor(JqaRuleType::class.java)

            override fun save(output: DataOutput, data: Collection<DeclarationStub>) {
                DataInputOutputUtilRt.writeSeq(output, data) { declaration ->
                    EnumeratorIntegerDescriptor.INSTANCE.save(output, declaration.offset)
                    enumDescriptor.save(output, declaration.type)
                }
            }

            override fun read(input: DataInput): Collection<DeclarationStub> =
                DataInputOutputUtilRt.readSeq(input) {
                    val offset = EnumeratorIntegerDescriptor.INSTANCE.read(input)
                    val ruleType = enumDescriptor.read(input)
                    DeclarationStub(offset, ruleType)
                }
        }

    override fun getVersion(): Int = Util.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(XmlFileType.INSTANCE)

    override fun dependsOnFileContent(): Boolean = true

    // Required for IntelliJ to filter keys by project when using [FileBasedIndex::getAllKeys].
    override fun traceKeyHashToVirtualFileMapping(): Boolean = true
}
