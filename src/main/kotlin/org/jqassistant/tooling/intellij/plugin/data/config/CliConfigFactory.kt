package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.commandline.task.AbstractRuleTask
import com.buschmais.jqassistant.core.resolver.api.MavenSettingsConfigSourceBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationMappingLoader
import com.intellij.openapi.components.service
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.SysPropConfigSource
import org.eclipse.microprofile.config.spi.ConfigSource
import org.jqassistant.tooling.intellij.plugin.common.withServiceLoader
import java.io.File
import kotlin.io.path.Path

class CliConfigFactory : JqaConfigFactory {
    override fun handlesDistribution(distribution: JqaDistribution) = distribution == JqaDistribution.CLI

    override fun assembleConfig(project: Project): FullArtifactConfiguration =
        // Use the CLI class loader to only pick up cli default plugins.
        withServiceLoader<AbstractRuleTask, _> {
            // Adapted from jQA

            val service = project.service<JqaConfigurationService>()

            val userHome = File(System.getProperty("user.home"))

            val commandLineOptions = service.parseCommandLine(service.cliParameters)

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

            val workingDirectory =
                service.cliExecutionRoot?.let { VfsUtil.virtualToIoFile(it) }
                    ?: VfsUtil.virtualToIoFile(project.getBaseDirectories().single())

            val defaultDirectory = Path(workingDirectory.absolutePath, "jqassistant", "rules")
            val defaultPathConfig =
                object : ConfigSource {
                    override fun getPropertyNames() = setOf("jqassistant.analyze.rule.directory")

                    override fun getValue(propertyName: String?) =
                        if (propertyName == "jqassistant.analyze.rule.directory") defaultDirectory.toString() else null

                    override fun getName() = "InlineDummy"

                    override fun getOrdinal() = 401
                }

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
