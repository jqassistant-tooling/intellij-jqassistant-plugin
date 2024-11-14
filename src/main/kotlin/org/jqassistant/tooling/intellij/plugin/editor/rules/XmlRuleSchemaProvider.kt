package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jqassistant.tooling.intellij.plugin.common.parseXsdTargetNamespace
import com.intellij.xml.XmlSchemaProvider
import com.jetbrains.rd.util.*


// We can't place those members in the companion object due to some IntelliJ Platform restrictions.

// TODO: Initialize lazily to improve startup speed.
private val schemas: Map<String, VirtualFile> = loadSchemas()

/**
 * Dynamically loads all xls schemas from the class path and extracts the corresponding namespaces.
 */
private fun loadSchemas(): Map<String, VirtualFile> {
    val classLoader = XmlRuleSchemaProvider::class.java.classLoader
    val uri = classLoader.getResource(XmlRuleSchemaProvider.SCHEMA_PATH) ?: return emptyMap()
    val dir = VfsUtil.findFileByURL(uri) ?: return emptyMap()

    val schemas = dir.children.mapNotNull { file ->
        if (file == null) return@mapNotNull null
        if (file.extension != "xsd") return@mapNotNull null
        val targetNamespace = parseXsdTargetNamespace(file) ?: return@mapNotNull null
        targetNamespace to file
    }.toMap()

    getLogger<XmlRuleSchemaProvider>().debug { "Loaded ${schemas.size} xls schemas." }

    return schemas
}


/**
 * Provides xml schemas, which IntelliJ uses for validation and completion.
 *
 * The schemas are loaded dynamically from the classpath.
 */
class XmlRuleSchemaProvider : XmlSchemaProvider() {
    companion object {
        /**
         * Resource path to search for xsd schemas.
         */
        const val SCHEMA_PATH = "META-INF/schema"
    }

    override fun getSchema(url: String, module: Module?, baseFile: PsiFile): XmlFile? {
        return schemas[url]?.let { virtualFile ->
            val psiFile = PsiManager.getInstance(baseFile.project).findFile(virtualFile)
            psiFile as? XmlFile
        }
    }

    override fun isAvailable(file: XmlFile): Boolean {
        return true
    }
}
