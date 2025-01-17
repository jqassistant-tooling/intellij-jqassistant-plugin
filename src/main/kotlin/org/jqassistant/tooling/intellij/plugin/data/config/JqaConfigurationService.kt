package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.core.rule.api.model.ConceptBucket
import com.buschmais.jqassistant.core.rule.api.model.ConstraintBucket
import com.buschmais.jqassistant.core.rule.api.model.GroupsBucket
import com.buschmais.jqassistant.core.rule.api.model.RuleSelection
import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.util.Optional
import java.util.Properties

enum class JqaDistribution {
    CLI,
    MAVEN,
}

data class CommandLineOptions(
    val configurationLocations: List<String>,
    val mavenSettings: Optional<File>,
    val profiles: List<String>,
    val properties: Properties,
)

@Service(Service.Level.PROJECT)
class JqaConfigurationService(
    val project: Project,
) {
    private val jqaConfigFileProvider = JqaConfigFileProvider(project)

    @Deprecated("Don't use for new stuff as this contains the old maven effective config logic.")
    val configProvider = JqaEffectiveConfigProvider(project, jqaConfigFileProvider)

    /**
     * The distribution of this jQA project, or `null` if the project is no jQA Project.
     */
    var distribution: JqaDistribution?

    /**
     * Additional parameters used when the cli distribution is active.
     */
    var cliParameters: String = ""

    /**
     * Execution root for the cli distribution.
     *
     * If `null` the project root is used.
     */
    var cliExecutionRoot: VirtualFile? = null

    /**
     * Specific maven pom of the maven project to use for the maven distribution.
     *
     * If `null` any maven project with the jqa plugin will be selected.
     */
    var mavenProjectPom: VirtualFile? = null

    /**
     * Additional parameters used when the maven distribution is active. They are parsed in the same format as
     * cli parameters, but only properties are supported.
     */
    var mavenParameters: String = ""

    /**
     * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
     */
    var mavenProjectDescription: String? = null

    /**
     * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
     */
    var mavenScriptSourceDirectory: String? = null

    /**
     * Used to compensate for a field of MavenProject that is not contained in the IntelliJ maven project model.
     */
    var mavenOutputEncoding: String? = null

    private fun gatherStandardOptions(): Options {
        // Adapted from jQA
        val options = Options()

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

        return options
    }

    fun parseCommandLine(args: String): CommandLineOptions {
        // Adapted from jQA

        val commandLine =
            DefaultParser().parse(
                gatherStandardOptions(),
                args.split(" ").toTypedArray(),
            )

        val profiles = commandLine.getOptionValues("-profiles").filter { it.isNotEmpty() }
        val locations = commandLine.getOptionValues("-configurationLocations").filter { it.isNotEmpty() }
        val mavenSettings = Optional.ofNullable(commandLine.getOptionValue("-mavenSettings")?.let { File(it) })
        val properties = commandLine.getOptionProperties("D")

        return CommandLineOptions(
            profiles = profiles,
            mavenSettings = mavenSettings,
            configurationLocations = locations,
            properties = properties,
        )
    }

    init {
        jqaConfigFileProvider.addFileEventListener(configProvider)

        // TODO: Expose as settings and use a better default strategy
        val hasPom = FilenameIndex.getVirtualFilesByName("pom.xml", ProjectScope.getProjectScope(project)).isNotEmpty()
        val hasJqaConfig =
            FilenameIndex.getVirtualFilesByName(".jqassistant.yaml", ProjectScope.getProjectScope(project)).isNotEmpty()

        distribution =
            when {
                hasPom && hasJqaConfig -> JqaDistribution.MAVEN
                hasJqaConfig -> JqaDistribution.CLI
                else -> null
            }
    }

    /**
     * Adds a listener that is notified when a config file changes.
     */
    fun addFileEventListener(listener: EventListener) {
        jqaConfigFileProvider.addFileEventListener(listener)
    }

    /**
     * Returns the active configuration of the jQA Project.
     */
    fun getConfiguration(): CliConfiguration? {
        val distribution = this.distribution ?: return null

        val factory =
            JqaConfigFactory.Util.EXTENSION_POINT.extensions
                .firstOrNull { it.handlesDistribution(distribution) } ?: return null

        return factory.assembleConfig(project) as CliConfiguration
    }

    /**
     * Get the rules currently available to jQA. This is very different to the rule index, which includes all rules
     * everywhere and disregards jQA config semantics.
     */
    fun getAvailableRules(): RuleSet =
        object : RuleSet {
            override fun getConceptBucket(): ConceptBucket = ConceptBucket()

            override fun getProvidedConcepts(): MutableMap<String, MutableSet<String>> = mutableMapOf()

            override fun getProvidingConcepts(): MutableMap<String, MutableSet<String>> = mutableMapOf()

            override fun getConstraintBucket(): ConstraintBucket = ConstraintBucket()

            override fun getGroupsBucket(): GroupsBucket = GroupsBucket()
        }

    /**
     * Get all files which are scanned by jQA for available rules.
     */
    fun getAvailableRuleSources(): List<VirtualFile> = emptyList()

    /**
     * Get the currently effective rules.
     */
    fun getEffectiveRules(): RuleSelection =
        RuleSelection.select(
            getAvailableRules(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
        )

    /**
     * Whether the maven distribution is technically supported in the current IDE setup.
     */
    fun isMavenDistributionSupported(): Boolean = true
}
