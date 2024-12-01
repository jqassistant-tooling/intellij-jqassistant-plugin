package org.jqassistant.tooling.intellij.plugin.data.plugin

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.commandline.plugin.ArtifactProviderFactory
import com.buschmais.jqassistant.core.runtime.api.configuration.ConfigurationMappingLoader
import com.buschmais.jqassistant.core.shared.configuration.Plugin
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import io.smallrye.config.SysPropConfigSource
import java.io.File
import kotlin.reflect.jvm.jvmName

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
        val plugins: Map<Plugin, List<File>>,
    )

    // TODO: Evaluate whether we can persist this.
    @Volatile
    private var cache: PluginCache =
        PluginCache(
            emptyMap(),
        )

    val plugins: Map<Plugin, List<File>> get() = cache.plugins

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
            getJqaConfiguration() ?: return thisLogger().error("Problem constructing jQA-Config for plugin sync.")

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

        val provider = ArtifactProviderFactory(userHome).create(config)

        val mappedPlugins =
            plugins.associateWith {
                provider.resolve(listOf(it))
            }

        cache =
            PluginCache(
                mappedPlugins,
            )

        ApplicationManager.getApplication().invokeLater {
            WriteAction.run<Throwable> {
                // TODO: Search for a stable alternative.
                AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(
                    project,
                    null,
                    emptyList(),
                    emptyList(),
                    JqaPluginService::class.jvmName,
                )
            }
        }
    }

    // TODO: Replace with configuration service that handles maven and everything config related.
    private fun getJqaConfiguration(): CliConfiguration? {
        val workingDirectory = project.basePath?.let { File(it) } ?: return null

        return ConfigurationMappingLoader
            .builder(CliConfiguration::class.java, listOf(".jqassistant.yaml"))
            .withUserHome(userHome)
            .withWorkingDirectory(workingDirectory)
            .withEnvVariables()
            .withClasspath()
            .load(SysPropConfigSource())
    }
}
