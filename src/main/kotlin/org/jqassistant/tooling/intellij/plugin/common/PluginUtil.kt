package org.jqassistant.tooling.intellij.plugin.common

import com.buschmais.jqassistant.core.rule.api.source.ClasspathRuleSource
import com.buschmais.jqassistant.core.runtime.api.plugin.PluginConfigurationReader
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jqassistant.schema.plugin.v2.JqassistantPlugin
import org.jqassistant.tooling.intellij.plugin.data.plugin.JqaPlugin
import java.io.File
import kotlin.io.path.Path

object PluginUtil {
    private const val PLUGIN_FILE_PATH = PluginConfigurationReader.PLUGIN_RESOURCE
    private const val PLUGIN_RULE_PATH = ClasspathRuleSource.RULE_RESOURCE_PATH

    fun readJqaPlugin(jarFile: File): JqaPlugin? = VfsUtil.findFileByIoFile(jarFile, true)?.let { readJqaPlugin(it) }

    fun readJqaPlugin(jarFile: VirtualFile): JqaPlugin? {
        val jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(jarFile) ?: return null
        val pluginFile =
            jarRoot.findFileByRelativePath(PLUGIN_FILE_PATH) ?: return null

        val pluginConfig = JaxbUtil.unmarshal<JqassistantPlugin>(pluginFile.inputStream)

        return JqaPlugin(
            jarRoot = jarRoot,
            rules =
                pluginConfig.rules?.resource?.map { ruleFile ->
                    Path(PLUGIN_RULE_PATH, ruleFile).toString()
                } ?: emptyList(),
            name = pluginConfig.name ?: jarFile.nameWithoutExtension,
        )
    }
}
