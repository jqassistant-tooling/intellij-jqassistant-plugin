package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.commandline.task.AbstractRuleTask
import com.buschmais.jqassistant.core.resolver.api.MavenSettingsConfigSourceBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationMappingLoader
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.SysPropConfigSource
import org.jqassistant.tooling.intellij.plugin.common.MyVfsUtil
import org.jqassistant.tooling.intellij.plugin.common.notifyBalloon
import org.jqassistant.tooling.intellij.plugin.common.withServiceLoader
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import org.jqassistant.tooling.intellij.plugin.settings.PluginSettings
import java.io.File
import kotlin.io.path.Path

class CliConfigFactory : JqaConfigFactory {
    override fun handlesDistribution(distribution: JqaDistribution) = distribution == JqaDistribution.CLI

    override fun assembleConfig(project: Project): FullArtifactConfiguration? =
        // Use the CLI class loader to only pick up cli default plugins.
        withServiceLoader<AbstractRuleTask, _> {
            // Adapted from jQA

            val settings = project.service<PluginSettings>().state

            val service = project.service<JqaConfigurationService>()

            val userHome = File(System.getProperty("user.home"))

            val commandLineOptions = service.parseCommandLine(settings.cliParams)

            val configurationBuilder = ConfigurationBuilder("TaskConfigSource", 200)

            val commandLineProperties =
                PropertiesConfigSource(commandLineOptions.properties, "Command line properties", 400)
            val mavenSettingsConfigSource =
                MavenSettingsConfigSourceBuilder.createMavenSettingsConfigSource(
                    userHome,
                    commandLineOptions.mavenSettings,
                    commandLineOptions.profiles,
                )
            val configSource = configurationBuilder.build()

            val baseDir = MyVfsUtil.findFileRelativeToProject(project, settings.cliExecRootDir)

            if (baseDir == null) {
                project.notifyBalloon(
                    MessageBundle.message("multi.root.project.not.supported"),
                    NotificationType.ERROR,
                )
                return null
            }

            val workingDirectory = VfsUtil.virtualToIoFile(baseDir)

            val defaultDirectory = Path(workingDirectory.absolutePath, "jqassistant", "rules")
            val defaultPathConfig =
                PropertiesConfigSource(
                    mapOf(
                        "jqassistant.analyze.rule.directory" to defaultDirectory.toString(),
                    ),
                    "InlineDummy",
                    401,
                )

            return FullArtifactConfigurationWrapper(
                ConfigurationMappingLoader
                    .builder(CliConfiguration::class.java, commandLineOptions.configurationLocations)
                    .withUserHome(userHome)
                    .withWorkingDirectory(workingDirectory)
                    .withClasspath()
                    .withEnvVariables()
                    .withProfiles(commandLineOptions.profiles)
                    .withIgnoreProperties(setOf("jqassistant.opts", "jqassistant.home"))
                    .load(
                        configSource,
                        SysPropConfigSource(),
                        commandLineProperties,
                        mavenSettingsConfigSource,
                        defaultPathConfig,
                    ),
            )
        }
}
