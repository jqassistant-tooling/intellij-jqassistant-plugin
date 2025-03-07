package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationBuilder
import com.buschmais.jqassistant.core.shared.configuration.ConfigurationMappingLoader
import com.buschmais.jqassistant.scm.maven.AbstractRuleMojo
import com.buschmais.jqassistant.scm.maven.configuration.source.EmptyConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.MavenProjectConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.MavenPropertiesConfigSource
import com.buschmais.jqassistant.scm.maven.configuration.source.SettingsConfigSource
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.source.yaml.YamlConfigSource
import org.apache.maven.model.Build
import org.apache.maven.project.MavenProject
import org.apache.maven.settings.Settings
import org.jdom.Element
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jqassistant.tooling.intellij.plugin.common.MyVfsUtil
import org.jqassistant.tooling.intellij.plugin.common.notifyBalloon
import org.jqassistant.tooling.intellij.plugin.common.withServiceLoader
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import org.jqassistant.tooling.intellij.plugin.settings.PluginSettings
import java.io.File
import java.util.Properties
import kotlin.io.path.Path

/**
 * Representation of the settings configurable through pom.xml
 *
 * Parsing is done manually since using maven mapping logic doesn't seem to be possible.
 */
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

/**
 * Adapts an IntelliJ Maven Project model as normal MavenProject which is usable by jQA.
 */
class MavenProjectAdapter(
    val project: org.jetbrains.idea.maven.project.MavenProject,
    private val description: String?,
    private val scriptSource: String?,
) : MavenProject() {
    override fun getName(): String? = project.name

    override fun getDescription(): String? = description

    override fun getGroupId(): String? = project.mavenId.groupId

    override fun getArtifactId(): String? = project.mavenId.artifactId

    override fun getVersion(): String? = project.mavenId.version

    override fun getPackaging(): String = project.packaging

    override fun getBasedir(): File = VfsUtil.virtualToIoFile(project.directoryFile)

    override fun getBuild(): Build =
        object : Build() {
            override fun getSourceDirectory(): String? = project.sources.singleOrNull()

            override fun getTestSourceDirectory(): String? = project.testSources.singleOrNull()

            override fun getScriptSourceDirectory(): String? = scriptSource

            override fun getDirectory(): String = project.directory

            override fun getOutputDirectory(): String = project.outputDirectory

            override fun getTestOutputDirectory(): String = project.testOutputDirectory

            override fun getFinalName(): String = project.finalName
        }
}

/**
 * Adapts an IntelliJ Maven Project as regular MavenSettings which are usable by jQA.
 */
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

    private fun MavenPlugin.isJqaPlugin() = groupId == JQA_MAVEN_PLUGIN_GROUP && artifactId == JQA_MAVEN_PLUGIN_ARTIFACT

    private fun findMavenProject(
        project: Project,
        settings: PluginSettings.State,
    ): Pair<org.jetbrains.idea.maven.project.MavenProject, JqaMavenConfiguration>? {
        val mavenProjectManager = MavenProjectsManager.getInstance(project)

        if (settings.mavenProjectFile != null) {
            // Check whether the selected maven project is valid.

            val projectFile =
                MyVfsUtil.findFileRelativeToProject(project, settings.mavenProjectFile)

            if (projectFile == null) {
                project.notifyBalloon(
                    MessageBundle.message("multi.root.project.not.supported"),
                    NotificationType.ERROR,
                )
                return null
            }

            val mavenProject = mavenProjectManager.findProject(projectFile)

            if (mavenProject == null) {
                project.notifyBalloon(
                    MessageBundle.message(
                        "invalid.maven.project",
                        projectFile.presentableUrl,
                    ),
                    NotificationType.ERROR,
                )
                return null
            }

            val resolvedProject = isJQAProject(mavenProject)

            if (resolvedProject == null) {
                project.notifyBalloon(
                    MessageBundle.message("maven.project.without.plugin", projectFile.presentableUrl),
                    NotificationType.ERROR,
                )
                return null
            } else {
                return resolvedProject
            }
        } else {
            // Search for an arbitrary project that contains the jQA Plugin.

            // Try root projects first.
            val availableProjects =
                mavenProjectManager.rootProjects +
                    (mavenProjectManager.projects - mavenProjectManager.rootProjects.toSet())

            for (mavenProject in availableProjects) {
                val resolvedProject = isJQAProject(mavenProject)
                if (resolvedProject != null) {
                    return resolvedProject
                }
            }
            project.notifyBalloon(MessageBundle.message("no.maven.project.with.plugin"), NotificationType.ERROR)
            return null
        }
    }

    private fun isJQAProject(
        project: org.jetbrains.idea.maven.project.MavenProject,
    ): Pair<org.jetbrains.idea.maven.project.MavenProject, JqaMavenConfiguration>? {
        val jqaMavenPlugin = project.plugins.firstOrNull { it.isJqaPlugin() }

        if (jqaMavenPlugin != null) {
            return Pair(
                project,
                JqaMavenConfiguration.fromJDomElement(jqaMavenPlugin.configurationElement),
            )
        } else if (project.directoryFile.children.firstOrNull {
                it.name == ".jqassistant.yaml" || it.name == ".jqassistant.yml"
            } != null
        ) {
            return Pair(
                project,
                JqaMavenConfiguration.fromJDomElement(null),
            )
        }
        return null
    }

    override fun assembleConfig(project: Project): FullArtifactConfiguration? =
        // Use a class loader of the maven plugin, to pick up maven default plugins.
        withServiceLoader<AbstractRuleMojo, _> {
            val settings = project.service<PluginSettings>().state

            val service = project.service<JqaConfigurationService>()

            val configurationBuilder = ConfigurationBuilder("MojoConfigSource", 110)

            // Find any maven project that has the jqa maven plugin activated, so that we can access the config.
            val (mavenProject, mavenConfig) = findMavenProject(project, settings) ?: return@withServiceLoader null

            val projectConfigSource =
                MavenProjectConfigSource(
                    MavenProjectAdapter(
                        mavenProject,
                        settings.mavenProjectDescription,
                        settings.mavenScriptSourceDir,
                    ),
                )

            val settingsConfigSource = SettingsConfigSource(SettingsAdapter(mavenProject))
            val projectPropertiesConfigSource =
                MavenPropertiesConfigSource(mavenProject.properties, "Maven Project Properties")

            val commandLineOptions =
                service.parseCommandLine(settings.mavenAdditionalProps)

            val userPropertiesConfigSource =
                MavenPropertiesConfigSource(commandLineOptions.properties, "Maven Session User Properties ")

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
                PropertiesConfigSource(
                    mapOf(
                        "jqassistant.analyze.rule.directory" to defaultDirectory.toString(),
                    ),
                    "InlineDummy",
                    401,
                )

            val configSources =
                arrayOf(
                    configurationBuilder.build(),
                    projectConfigSource,
                    settingsConfigSource,
                    projectPropertiesConfigSource,
                    userPropertiesConfigSource,
                    yamlConfiguration,
                    propertiesConfiguration,
                    defaultPathConfig,
                )

            val userHome = File(System.getProperty("user.home"))

            val executionRootDirectory = VfsUtil.virtualToIoFile(project.baseDir)
            val builder =
                ConfigurationMappingLoader
                    .builder(
                        // Since [MavenConfiguration] doesn't implement [ArtifactResolverConfiguration] we can't use it
                        // at the moment. This might lead to problems with custom repository setups configured in maven.
                        // But it'll probably work since repository settings from the maven settings file are also
                        // picked up by the CLI.
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
            return@withServiceLoader FullArtifactConfigurationWrapper(builder.load(*configSources))
        }
}
