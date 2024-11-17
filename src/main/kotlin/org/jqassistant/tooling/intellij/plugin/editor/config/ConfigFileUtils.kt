package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.elementType
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes

/**
 * Utilities regarding jQA-Config files.
 */
object ConfigFileUtils {
    /**
     * Key to store whether a [VirtualFile] is a jQA config file. Can be used to mark embedded files as jQA config.
     */
    val JQA_CONFIG_FILE_KEY = KeyWithDefaultValue.create("jqassistant.is_config_file", false)

    /**
     * Checks whether a [VirtualFile] is a jQA config file.
     *
     * This method should be used to decide whether features like validation apply to the file, since it applies as many
     * checks as possible to test whether this file could be meant to be a jQA config. This method gives no information
     * on whether the file is part of the current effective config. TODO: Document correct method for this purpose.
     */
    fun isJqaConfigFile(file: VirtualFile, project: Project? = null): Boolean {
        if (file.isDirectory || !file.isValid) return false

        // Always return `true` if the file is marked as jQA config file.
        if (file.getUserData(JQA_CONFIG_FILE_KEY) == true) return true

        // The file uses a common name for jQA config files.
        if (file.fileType == YAMLFileType.YML && file.nameWithoutExtension == ".jqassistant") return true

        // TODO: Move to indexing and use user data to store results for quick access.
        if (project != null) {
            // TODO: The file is part of the effective jQA configuration.

            // The file is a yaml file and contains a `jqassistant` top level key.

            val viewProvider = PsiManager.getInstance(project).findViewProvider(file) ?: return false
            val psiFile = viewProvider.getPsi(YAMLLanguage.INSTANCE) ?: return false

            // If there are problems with tree loading restrictions, use `AstLoadingFilter.forceAllowTreeLoading`.
            // If there are performance problems, use `LighterAST`.
            for (document in psiFile.children) {
                if (document.elementType == YAMLElementTypes.DOCUMENT) {
                    for (mapping in document.children) {
                        if (mapping.elementType == YAMLElementTypes.MAPPING) {
                            for (keyPair in mapping.children) {
                                if (keyPair.elementType == YAMLElementTypes.KEY_VALUE_PAIR) {
                                    val key = keyPair.firstChild ?: break
                                    if (key.elementType == YAMLTokenTypes.SCALAR_KEY && key.text == "jqassistant") return true
                                }
                            }
                        }
                    }
                }
            }
        }

        return false
    }
}
