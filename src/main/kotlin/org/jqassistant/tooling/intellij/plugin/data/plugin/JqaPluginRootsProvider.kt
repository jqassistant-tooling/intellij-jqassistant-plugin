package org.jqassistant.tooling.intellij.plugin.data.plugin

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * Adapter to present an [JqaPlugin] as [SyntheticLibrary] to IntelliJ.
 *
 * Will be displayed in the external libraries view of the project pane.
 */
class JqaPluginLibraryAdapter(
    private val plugin: JqaPlugin,
) : SyntheticLibrary(),
    ItemPresentation {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherLib = other as? JqaPluginLibraryAdapter ?: return false
        return otherLib.plugin == plugin
    }

    override fun hashCode(): Int = plugin.hashCode()

    override fun getSourceRoots(): MutableCollection<VirtualFile> = mutableListOf(plugin.jarRoot)

    override fun getPresentableText(): String = "jQA: ${plugin.name}"

    override fun getIcon(unused: Boolean): Icon = AllIcons.Nodes.PpLibFolder
}

/**
 * Provides jQA-Plugins in form of [JqaPluginLibraryAdapter] to IntelliJ.
 *
 * See [JqaPluginService] for how jQA-Plugins are managed and how this gets updated.
 */
class JqaPluginRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> =
        project.service<JqaPluginService>().pluginJars.map { root ->
            JqaPluginLibraryAdapter(root)
        }
}
