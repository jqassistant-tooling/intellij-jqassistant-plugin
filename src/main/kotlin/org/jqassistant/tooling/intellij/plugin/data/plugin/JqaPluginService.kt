package org.jqassistant.tooling.intellij.plugin.data.plugin

import com.buschmais.jqassistant.core.resolver.api.ArtifactProviderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jqassistant.tooling.intellij.plugin.common.PluginUtil
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import java.io.File
import kotlin.reflect.jvm.jvmName

/**
 * Representation of jQA-Plugin for use in IntelliJ.
 *
 * Contains information about the jar, and plugin details which are extracted from the jar and saved for quick access.
 *
 * @see [PluginUtil.readJqaPlugin]
 */
class JqaPlugin(
    /**
     * A [VirtualFile] representing the jar root of a plugin.
     *
     * This [VirtualFile] should have the jar contents accessible, see [JarFileSystem.getJarRootForLocalFile].
     */
    val jarRoot: VirtualFile,
    /**
     * Names of rule files which this plugin has.
     *
     * The names are relative to the jar (meaning they contain the `META-INF/jqassistant-rules` prefix), this
     * means they can be resolved against the [jarRoot] by using [VirtualFile.findFileByRelativePath].
     */
    val rules: List<String>,
    /**
     * Human-readable name of the plugin.
     *
     * Usually the name is configured through the plugin config file.
     */
    val name: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherPlugin = other as? JqaPlugin ?: return false
        return jarRoot == otherPlugin.jarRoot
    }

    override fun hashCode(): Int = jarRoot.hashCode()
}

/**
 * Handles installation and localization of jQA-Plugins.
 *
 * Keeps a cache of active plugins and corresponding jar locations.
 */
@Service(Service.Level.PROJECT)
class JqaPluginService(
    private val project: Project,
) {
    private val userHome = File(System.getProperty("user.home"))

    private data class PluginCache(
        val pluginJars: List<JqaPlugin>,
    )

    // TODO: Evaluate whether we can persist this.
    @Volatile
    private var cache: PluginCache =
        PluginCache(
            emptyList(),
        )

    val pluginJars: List<JqaPlugin> get() = cache.pluginJars

    /**
     * Synchronizes jQA-Plugins based on the current configuration.
     *
     * This methods might install plugins from the internet if they
     * are not present so it should be called from background threads,
     * and with a progress indicator if possible.
     */
    @Synchronized
    fun synchronizePlugins() {
        val config =
            project.service<JqaConfigurationService>().getConfiguration()
                ?: return thisLogger().error("Problem constructing jQA-Config for plugin sync.")

        // TODO: Figure out builtin plugins.
        val plugins =
            (config.defaultPlugins() + config.plugins()).filter {
                if (it.type() == "jar") {
                    true
                } else {
                    thisLogger().error("Non jar based jQA-Plugins are not supported.")
                    false
                }
            }

        val provider = ArtifactProviderFactory.getArtifactProvider(config, userHome)

        val jars = provider.resolve(plugins)

        val pluginJars =
            jars.mapNotNull { jar ->
                PluginUtil.readJqaPlugin(jar)
            }

        cache =
            PluginCache(
                pluginJars,
            )

        // Files from the plugin that IntelliJ will index.
        val newRoots =
            pluginJars.flatMap { plugin ->
                plugin.rules.map { ruleFile ->
                    plugin.jarRoot.findFileByRelativePath(
                        ruleFile,
                    )
                }
            }

        ApplicationManager.getApplication().invokeLater {
            WriteAction.run<Throwable> {
                // TODO: Search for a stable alternative.
                AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
                    project,
                    null,
                    emptyList(),
                    newRoots,
                    JqaPluginService::class.jvmName,
                )
            }
        }
    }
}
