package org.jqassistant.tooling.intellij.plugin.data.plugin

import com.buschmais.jqassistant.core.shared.configuration.Plugin
import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.Icon

/**
 * A synthetic library representing an jQA-Plugin.
 *
 * Will be displayed in the external libraries view of the project pane.
 */
class JqaPluginLibrary(
    private val plugin: Plugin,
    private val roots: List<File>,
) : SyntheticLibrary(),
    ItemPresentation {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val otherLib = other as? JqaPluginLibrary ?: return false

        return otherLib.plugin == plugin
    }

    override fun hashCode(): Int = plugin.hashCode()

    override fun getSourceRoots(): MutableCollection<VirtualFile> {
        val virtualFiles = roots.mapNotNull { VfsUtil.findFileByIoFile(it, true) }
        val resolvedJars =
            virtualFiles.mapTo(mutableListOf()) {
                JarFileSystem.getInstance().getJarRootForLocalFile(it) ?: it
            }

        return resolvedJars
    }

    // TODO: Can [artifactId()] contain multiple artifacts?
    override fun getPresentableText(): String = "jQA: ${plugin.groupId()}:${plugin.artifactId()[0]}:${plugin.version()}"

    override fun getIcon(unused: Boolean): Icon = AllIcons.Nodes.PpLibFolder
}

/**
 * Provides jQA-Plugins in form of [JqaPluginLibrary] to IntelliJ.
 *
 * See [JqaPluginService] for how jQA-Plugins are managed and how this gets updated.
 */
class JqaPluginRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> =
        project.service<JqaPluginService>().plugins.map { (plugin, roots) ->
            JqaPluginLibrary(plugin, roots)
        }
}
