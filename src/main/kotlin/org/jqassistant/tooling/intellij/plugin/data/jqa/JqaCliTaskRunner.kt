package org.jqassistant.tooling.intellij.plugin.data.jqa

import com.buschmais.jqassistant.commandline.CliConfigurationException
import com.buschmais.jqassistant.commandline.CliExecutionException
import com.buschmais.jqassistant.commandline.Main
import com.buschmais.jqassistant.commandline.Task
import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.commandline.configuration.MavenSettingsConfigSourceBuilder
import com.buschmais.jqassistant.commandline.plugin.ArtifactProviderFactory
import com.buschmais.jqassistant.commandline.task.RegisteredTask
import com.buschmais.jqassistant.core.runtime.api.configuration.ConfigurationBuilder
import com.buschmais.jqassistant.core.runtime.api.configuration.ConfigurationMappingLoader
import com.buschmais.jqassistant.core.runtime.api.plugin.PluginConfigurationReader
import com.buschmais.jqassistant.core.runtime.api.plugin.PluginRepository
import com.buschmais.jqassistant.core.runtime.api.plugin.PluginResolver
import com.buschmais.jqassistant.core.runtime.impl.plugin.PluginConfigurationReaderImpl
import com.buschmais.jqassistant.core.runtime.impl.plugin.PluginRepositoryImpl
import com.buschmais.jqassistant.core.runtime.impl.plugin.PluginResolverImpl
import com.buschmais.jqassistant.core.shared.artifact.ArtifactProvider
import com.buschmais.jqassistant.core.store.api.StoreFactory
import com.intellij.openapi.project.Project
import io.smallrye.config.PropertiesConfigSource
import io.smallrye.config.SysPropConfigSource
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.jqassistant.tooling.intellij.plugin.data.jqa.tasks.CustomRuleTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Arrays
import java.util.Optional
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class JqaCliTaskRunner(
    val project: Project,
) {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(Main::class.java)

        private const val CMDLINE_OPTION_CONFIG_LOCATIONS: String = "-configurationLocations"

        private const val CMDLINE_OPTION_MAVEN_SETTINGS: String = "-mavenSettings"

        private const val CMDLINE_OPTION_PROFILES: String = "-profiles"

        private val IGNORE_PROPERTIES: Set<String> =
            setOf("jqassistant.opts", "jqassistant.home") //  env variables provided by jqassistant shell scripts
    }

    /**
     * The main class, i.e. the entry point for the CLI.
     *
     * @author jn4, Kontext E GmbH, 23.01.14
     * @author Dirk Mahler
     * @author altered by Valentin Ehrhardt
     */

    @Throws(CliExecutionException::class)
    fun <T> runTask(taskClass: KClass<out CustomRuleTask<T>>): T? {
        val task = taskClass.createInstance()
        val options = gatherOptions()
        val commandLine =
            getCommandLine(arrayOf("-C", "C:\\Users\\Valentin\\IdeaProjects\\sig-metrics\\.jqassistant.yaml"), options)
        val triple = prepareTask(task, commandLine, options) ?: return null
        val pluginRepository = triple.first
        val storeFactory = triple.second
        val configuration = triple.third

        val contextClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = pluginRepository.classLoader
        try {
            task.initialize(pluginRepository, storeFactory)
            task.run(configuration, options)
        } finally {
            Thread.currentThread().contextClassLoader = contextClassLoader
            pluginRepository.destroy()
        }

        return task.getResult()
    }

    /**
     * Initialize the plugin repository.
     *
     * @param configuration
     * The [CliConfiguration]
     * @param artifactProvider
     * The [ArtifactProvider]
     * @return The repository.
     */
    private fun getPluginRepository(
        configuration: CliConfiguration,
        artifactProvider: ArtifactProvider,
    ): PluginRepository {
        val pluginResolver: PluginResolver = PluginResolverImpl(artifactProvider)
        val pluginClassLoader =
            pluginResolver.createClassLoader(
                Task::class.java.classLoader,
                configuration,
            )
        val pluginConfigurationReader: PluginConfigurationReader = PluginConfigurationReaderImpl(pluginClassLoader)
        val pluginRepository = PluginRepositoryImpl(pluginConfigurationReader)
        pluginRepository.initialize()
        return pluginRepository
    }

    /**
     * Gather all options which are supported by the task (i.e. including standard and specific options).
     *
     * @return The options.
     */
    private fun gatherOptions(): Options {
        val options = Options()
        gatherTasksOptions(options)
        gatherStandardOptions(options)
        return options
    }

    /**
     * Gathers the standard options shared by all tasks.
     *
     * @param options
     * The standard options.
     */
    private fun gatherStandardOptions(options: Options) {
        options.addOption(
            Option
                .builder("C")
                .longOpt("configurationLocations")
                .desc("The list of configuration locations, i.e. YAML files and directories")
                .hasArgs()
                .valueSeparator(',')
                .build(),
        )
        options.addOption(
            Option
                .builder("M")
                .longOpt("mavenSettings")
                .desc(
                    "The location of a Maven settings.xml file to use for repository, proxy and mirror configurations.",
                ).hasArg()
                .valueSeparator(',')
                .build(),
        )
        options.addOption(
            Option
                .builder("P")
                .longOpt("profiles")
                .desc("The configuration profiles to activate.")
                .hasArgs()
                .valueSeparator(',')
                .build(),
        )
        options.addOption(
            Option
                .builder("D")
                .desc("Additional configuration property.")
                .hasArgs()
                .valueSeparator('=')
                .build(),
        )
    }

    /**
     * Gathers the task specific options for all tasks.
     *
     * @param options
     * The task specific options.
     */
    private fun gatherTasksOptions(options: Options) {
        for (task in RegisteredTask.getTasks()) {
            for (option in task.options) {
                options.addOption(option)
            }
        }
    }

    /**
     * Parse the command line and execute the requested task.
     *
     * @param commandLine
     * The command line.
     * @param options
     * The known options.
     * @throws CliExecutionException
     * If an error occurs.
     */
    @Throws(CliExecutionException::class)
    private fun prepareTask(
        task: Task,
        commandLine: CommandLine,
        options: Options,
    ): Triple<PluginRepository, StoreFactory, CliConfiguration>? {
        val workingDirectory = project.basePath?.let { File(it) } ?: return null
        val userHome = File(System.getProperty("user.home"))
        val configuration = getCliConfiguration(commandLine, workingDirectory, userHome, listOf(task))
        if (configuration.skip()) {
            LOGGER.info("Skipping execution.")
        } else {
            val artifactProviderFactory = ArtifactProviderFactory(userHome)
            val artifactProvider: ArtifactProvider = artifactProviderFactory.create(configuration)
            val originalClassLoader = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = this::class.java.classLoader
            var pluginRepository: PluginRepository? = null
            try {
                // Get PluginRepository
                pluginRepository = getPluginRepository(configuration, artifactProvider)
            } finally {
                // Restore the original class loader
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
            if (pluginRepository == null) {
                return null
            }

            val storeFactory = StoreFactory(pluginRepository.storePluginRepository, artifactProvider)
            task.initialize(pluginRepository, storeFactory)

            return Triple(pluginRepository, storeFactory, configuration)
        }
        return null
    }

    @Throws(CliConfigurationException::class)
    private fun getCliConfiguration(
        commandLine: CommandLine,
        workingDirectory: File,
        userHome: File,
        tasks: List<Task>,
    ): CliConfiguration {
        val configLocations = getConfigLocations(commandLine)
        val profiles = getUserProfiles(commandLine)
        val configurationBuilder = ConfigurationBuilder("TaskConfigSource", 200)
        val properties =
            commandLine
                .getOptionProperties("D")
                .entries
                .stream()
                .collect(
                    Collectors.toMap<Map.Entry<Any, Any>, String, String>(
                        { entry: Map.Entry<Any, Any> -> entry.key.toString() },
                        { entry: Map.Entry<Any, Any> -> entry.value.toString() },
                    ),
                )
        val commandLineProperties = PropertiesConfigSource(properties, "Command line properties", 400)
        val mavenSettingsConfigSource =
            MavenSettingsConfigSourceBuilder.createMavenSettingsConfigSource(
                userHome,
                getMavenSettings(commandLine),
                profiles,
            )
        for (task in tasks) {
            task.configure(commandLine, configurationBuilder)
        }
        val configSource = configurationBuilder.build()
        return ConfigurationMappingLoader
            .builder<CliConfiguration>(CliConfiguration::class.java, configLocations)
            .withUserHome(userHome)
            .withWorkingDirectory(workingDirectory)
            .withClasspath()
            .withEnvVariables()
            .withProfiles(profiles)
            .withIgnoreProperties(IGNORE_PROPERTIES)
            .load(configSource, SysPropConfigSource(), commandLineProperties, mavenSettingsConfigSource)
    }

    private fun getConfigLocations(commandLine: CommandLine): List<String> {
        // does the instantiator always tell us where the config files are?
        if (commandLine.hasOption(
                CMDLINE_OPTION_CONFIG_LOCATIONS,
            )
        ) {
            return Arrays
                .stream<String>(
                    commandLine.getOptionValues(
                        CMDLINE_OPTION_CONFIG_LOCATIONS,
                    ),
                ).filter { configLocation: String -> configLocation.isNotEmpty() }
                .collect(Collectors.toList<String>())
        }
        // default config file, but we should try to get all config files always
        return listOf()
    }

    private fun getMavenSettings(commandLine: CommandLine): Optional<File> {
        if (commandLine.hasOption(CMDLINE_OPTION_MAVEN_SETTINGS)) {
            return Optional.of<File>(
                File(
                    commandLine.getOptionValue(
                        CMDLINE_OPTION_MAVEN_SETTINGS,
                    ),
                ),
            )
        }
        return Optional.empty()
    }

    private fun getUserProfiles(commandLine: CommandLine): List<String> {
        if (commandLine.hasOption(CMDLINE_OPTION_PROFILES)) {
            return Arrays
                .stream<String>(
                    commandLine.getOptionValues(
                        CMDLINE_OPTION_PROFILES,
                    ),
                ).filter { userProfile: String -> userProfile.isNotEmpty() }
                .collect(Collectors.toList<String>())
        }
        return emptyList()
    }

    /**
     * Parse the command line
     *
     * @param args
     * The arguments.
     * @param options
     * The known options.
     * @return The command line.
     */
    @Throws(CliExecutionException::class)
    private fun getCommandLine(args: Array<String>, options: Options): CommandLine {
        val parser: CommandLineParser = DefaultParser()
        try {
            return parser.parse(options, args)
        } catch (e: ParseException) {
            throw CliExecutionException("Cannot parse command line arguments", e)
        }
    }
}
