package org.jqassistant.tooling.intellij.plugin.data.plugin

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.io.DataInputOutputUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

/**
 * Indexes rules from a plugin jar by name.
 *
 * Uses the plugin description to find relevant [VirtualFile]s from the jar and indexes them with the same extraction
 * strategies that the xml rule index uses.
 */
class NameIndex : FileBasedIndexExtension<String, Collection<NameIndex.JarRule>>() {
    object Util {
        val NAME = ID.create<String, Collection<JarRule>>("jqassistant.plugin.NameIndex")
        const val VERSION = 1
    }

    /**
     * Indexed representation of a rule.
     *
     * [fileName] a path in the jar, relative to `META-INF/jqassistant-rules` (no guarantees regarding leading slashes).
     * [offset] offset of the [PsiElement] in its file.
     */
    data class JarRule(
        val fileName: String,
        val offset: Int,
    )

    private object Externalizer : DataExternalizer<Collection<JarRule>> {
        override fun save(out: DataOutput, value: Collection<JarRule>) {
            DataInputOutputUtilRt.writeSeq(out, value) { rule ->
                DataInputOutputUtilRt.writeINT(out, rule.offset)
                out.writeUTF(rule.fileName)
            }
        }

        override fun read(input: DataInput): Collection<JarRule> =
            DataInputOutputUtilRt.readSeq(input) {
                val offset = DataInputOutputUtilRt.readINT(input)
                val file = input.readUTF()
                JarRule(
                    offset = offset,
                    fileName = file,
                )
            }
    }

    override fun getName(): ID<String, Collection<JarRule>> = Util.NAME

    override fun getIndexer(): DataIndexer<String, Collection<JarRule>, FileContent> =
        object : DataIndexer<String, Collection<JarRule>, FileContent> {
            override fun map(content: FileContent): MutableMap<String, Collection<JarRule>> {
                thisLogger().warn(content.fileName)

                /*// TODO: Reject files based on the effective configuration.
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
                }*/

                return mutableMapOf()
            }
        }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Collection<JarRule>> = Externalizer

    override fun getVersion(): Int = Util.VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(ArchiveFileType.INSTANCE)

    override fun dependsOnFileContent(): Boolean = true
}
