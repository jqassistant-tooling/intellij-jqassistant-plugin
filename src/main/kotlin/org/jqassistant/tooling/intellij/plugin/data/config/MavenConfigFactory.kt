package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.core.runtime.api.configuration.Configuration
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationMappingLoader
import com.buschmais.jqassistant.scm.maven.configuration.source.EmptyConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.MavenProjectConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.MavenPropertiesConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.SettingsConfigSource
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.source.yaml.YamlConfigSource
import org.apache.maven.model.Build
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.eclipse.microprofile.config.spi.ConfigSource
import org.jdom.Element
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jqassistant.tooling.intellij.plugin.common.withServiceLoader
import java.io.File
import java.util.Properties
import kotlin.io.path.Path

data class JqaMavenConfiguration(
    val yaml: String?,
    val configurationLocations: Set<String>,
    val properties: Properties,
) {
    companion object {
        fun fromJDomElement(elem: Element?): JqaMavenConfiguration {
            // Would be great if we could use the same mapping logic as maven, but that doesn't really seem possible.

            val yaml = elem?.getChild("yaml")?.text
            val configurationLocations =
                elem
                    ?.getChild("configurationLocations")
                    ?.getChildren("configurationLocation")
                    ?.mapNotNullTo(mutableSetOf()) { it?.text } ?: emptySet()

            val properties = Properties()
            elem?.getChild("properties")?.children?.forEach { element ->
                properties[element.name] = element.value
            }

            return JqaMavenConfiguration(yaml, configurationLocations, properties)
        }
    }
}

class MavenProjectAdapter(
    val project: org.jetbrains.idea.maven.project.MavenProject,
) : MavenProject() {
    override fun getName(): String? = project.name

    override fun getDescription(): String? = null

    override fun getGroupId(): String? = project.mavenId.groupId

    override fun getArtifactId(): String? = project.mavenId.artifactId

    override fun getVersion(): String? = project.mavenId.version

    override fun getPackaging(): String = project.packaging

    override fun getBasedir(): File = VfsUtil.virtualToIoFile(project.directoryFile)

    override fun getBuild(): Build =
        object : Build() {
            override fun getDirectory(): String = project.directory

            override fun getOutputDirectory(): String = project.outputDirectory

            override fun getTestOutputDirectory(): String = project.testOutputDirectory

            override fun getFinalName(): String = project.finalName
        }
}

class SettingsAdapter(
    val project: org.jetbrains.idea.maven.project.MavenProject,
) : Settings() {
    override fun getLocalRepository(): String = project.localRepository.absolutePath
}

class MavenConfigFactory : JqaConfigFactory {
    companion object {
        const val JQA_MAVEN_PLUGIN_GROUP = "com.buschmais.jqassistant"
        const val JQA_MAVEN_PLUGIN_ARTIFACT = "jqassistant-maven-plugin"
    }

    override fun handlesDistribution(distribution: JqaDistribution) = distribution == JqaDistribution.MAVEN

    override fun createConfig(project: Project): Configuration? =
        withServiceLoader {
            val configurationBuilder = ConfigurationBuilder("MojoConfigSource", 110)

            val mavenProjectManager = MavenProjectsManager.getInstance(project)

            // Find any maven project that has the jqa maven plugin activated, so that we can access the config.
            val (mavenProject, mavenConfig) =
                run {
                    for (mavenProject in mavenProjectManager.rootProjects) {
                        val jqaMavenPlugin =
                            mavenProject.plugins.firstOrNull { plugin ->
                                plugin.groupId == JQA_MAVEN_PLUGIN_GROUP &&
                                    plugin.artifactId == JQA_MAVEN_PLUGIN_ARTIFACT
                            }
                                ?: continue
                        return@run mavenProject to
                            JqaMavenConfiguration.fromJDomElement(jqaMavenPlugin.configurationElement)
                    }
                    return@withServiceLoader null
                }

            val projectConfigSource = MavenProjectConfigSource(MavenProjectAdapter(mavenProject))
            val settingsConfigSource = SettingsConfigSource(SettingsAdapter(mavenProject))
            val projectPropertiesConfigSource =
                MavenPropertiesConfigSource(mavenProject.properties, "Maven Project Properties")

            // val userPropertiesConfigSource: MavenPropertiesConfigSource = MavenPropertiesConfigSource(session.getUserProperties(), "Maven Session User Properties ")
            // val systemPropertiesConfigSource: MavenPropertiesConfigSource = MavenPropertiesConfigSource( session.getSystemProperties(), "Maven Session System Properties")

            val yamlConfiguration =
                mavenConfig.yaml?.let { yaml ->
                    YamlConfigSource(
                        "Maven plugin execution YAML configuration",
                        yaml,
                        MavenPropertiesConfigSource.CONFIGURATION_ORDINAL_MAVEN_PROPERTIES,
                    )
                }
                    ?: EmptyConfigSource.INSTANCE

            val propertiesConfiguration =
                PropertiesConfigSource(
                    mavenConfig.properties,
                    "Maven plugin execution properties configuration",
                    MavenPropertiesConfigSource.CONFIGURATION_ORDINAL_MAVEN_PROPERTIES,
                )

            val mavenDir = VfsUtil.virtualToIoFile(mavenProject.directoryFile).absolutePath
            val defaultDirectory = Path(mavenDir, "jqassistant")

            val defaultPathConfig =
                object : ConfigSource {
                    override fun getPropertyNames() = setOf("jqassistant.analyze.rule.directory")

                    override fun getValue(propertyName: String?) =
                        if (propertyName == "jqassistant.analyze.rule.directory") defaultDirectory.toString() else null

                    override fun getName() = "InlineDummy"

                    override fun getOrdinal() = 401
                }

            val configSources =
                arrayOf(
                    configurationBuilder.build(),
                    projectConfigSource,
                    settingsConfigSource,
                    projectPropertiesConfigSource,
                    // userPropertiesConfigSource,
                    // systemPropertiesConfigSource,
                    yamlConfiguration,
                    propertiesConfiguration,
                    defaultPathConfig,
                )

            val userHome = File(System.getProperty("user.home"))

            val executionRootDirectory = VfsUtil.virtualToIoFile(project.baseDir)
            val builder =
                ConfigurationMappingLoader
                    .builder(
                        CliConfiguration::class.java,
                        mavenConfig.configurationLocations.toList(),
                    ).withUserHome(userHome)
                    .withDirectory(VfsUtil.virtualToIoFile(mavenProject.directoryFile), 100)
                    .withEnvVariables()
                    .withClasspath()
                    .withProfiles(
                        mavenProject.activatedProfilesIds.enabledProfiles.toList(),
                    ).withIgnoreProperties(setOf("jqassistant.configuration.locations"))
                    .withWorkingDirectory(executionRootDirectory)
            return@withServiceLoader builder.load(*configSources)
        }
}
