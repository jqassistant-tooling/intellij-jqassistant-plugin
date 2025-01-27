package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.core.resolver.api.ArtifactProviderFactory
import com.buschmais.jqassistant.core.rule.api.executor.CollectRulesVisitor
import com.buschmais.jqassistant.core.rule.api.model.ConceptBucket
import com.buschmais.jqassistant.core.rule.api.model.ConstraintBucket
import com.buschmais.jqassistant.core.rule.api.model.GroupsBucket
import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.buschmais.jqassistant.core.runtime.api.bootstrap.PluginRepositoryFactory
import com.buschmais.jqassistant.core.runtime.api.bootstrap.RuleProvider
import com.buschmais.jqassistant.core.runtime.api.plugin.PluginRepository
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.jqassistant.tooling.intellij.plugin.common.notifyBalloon
import org.jqassistant.tooling.intellij.plugin.common.withServiceLoader
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import org.jqassistant.tooling.intellij.plugin.settings.PluginSettings
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
) {
    companion object {
        val EMPTY =
            CommandLineOptions(
                emptyList(),
                Optional.empty(),
                emptyList(),
                Properties(),
            )
    }
}

/**
 * Allows to listen to config synchronization.
 *
 * Can be used to derive cached values from the resolved jQA Config during the sync. At the point when
 * listeners are notified, it is guaranteed that methods of [JqaConfigurationService]
 * will yield results that fit the config.
 */
fun interface JqaSyncListener {
    companion object {
        val TOPIC = Topic.create("jQA Sync Listener", JqaSyncListener::class.java)
    }

    fun synchronize(config: FullArtifactConfiguration?)
}

@Service(Service.Level.PROJECT)
class JqaConfigurationService(
    val project: Project,
) {
    class State {
        var dirty = false

        var effectiveConfiguration: FullArtifactConfiguration? = null
        var availableRuleSources: List<VirtualFile> = emptyList()
        var availableRules: RuleSet? = null
        var effectiveRules: CollectRulesVisitor? = null
    }

    @Volatile
    private var state = State()

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

    fun parseCommandLine(args: String?): CommandLineOptions {
        if (args.isNullOrEmpty()) {
            return CommandLineOptions.EMPTY
        }

        // Adapted from jQA

        val commandLine =
            try {
                DefaultParser().parse(
                    gatherStandardOptions(),
                    args.split(" ").toTypedArray(),
                )
            } catch (e: Exception) {
                project.notifyBalloon(MessageBundle.message("invalid.cmd.options", args), NotificationType.ERROR)
                return CommandLineOptions.EMPTY
            }

        val profiles = commandLine.getOptionValues("-profiles")?.filter { it.isNotEmpty() } ?: emptyList()
        val locations =
            commandLine.getOptionValues("-configurationLocations")?.filter { it.isNotEmpty() } ?: emptyList()
        val mavenSettings = Optional.ofNullable(commandLine.getOptionValue("-mavenSettings")?.let { File(it) })
        val properties = commandLine.getOptionProperties("D")

        return CommandLineOptions(
            profiles = profiles,
            mavenSettings = mavenSettings,
            configurationLocations = locations,
            properties = properties,
        )
    }

    fun synchronize() {
        val settings = project.service<PluginSettings>().state

        val factory =
            JqaConfigFactory.Util.EXTENSION_POINT.extensions
                .firstOrNull { it.handlesDistribution(settings.distribution) } ?: return

        val config = factory.assembleConfig(project)

        val ruleProvider =
            withServiceLoader {
                try {
                    val artifactProvider =
                        ArtifactProviderFactory.getArtifactProvider(config, File(System.getProperty("user.home")))

                    val pluginRepository: PluginRepository =
                        PluginRepositoryFactory.getPluginRepository(
                            config,
                            javaClass.classLoader,
                            artifactProvider,
                        )

                    RuleProvider.create(config, "", pluginRepository)
                } catch (e: Exception) {
                    project.notifyBalloon(
                        MessageBundle.message("jqa.exception", e.message ?: "TwT"),
                        NotificationType.ERROR,
                    )
                    null
                }
                // Default directories are handled through the config, since the plugin needs to make them absolute based on the maven project.
            } ?: return

        state.effectiveConfiguration = config
        state.availableRuleSources = ruleProvider.ruleSources.mapNotNull { VfsUtil.findFileByURL(it.url) }
        state.availableRules = ruleProvider.availableRules
        state.effectiveRules = ruleProvider.effectiveRules

        project.messageBus.syncPublisher(JqaSyncListener.TOPIC).synchronize(config)

        project.notifyBalloon(MessageBundle.message("synchronized.jqa.config"))
    }

    fun getConfiguration(): FullArtifactConfiguration? = state.effectiveConfiguration

    /**
     * Get the rules currently available to jQA. This is very different to the rule index, which includes all rules
     * everywhere and disregards jQA config semantics.
     */
    fun getAvailableRules(): RuleSet =
        state.availableRules ?: object : RuleSet {
            override fun getConceptBucket(): ConceptBucket = ConceptBucket()

            override fun getProvidedConcepts(): MutableMap<String, MutableSet<String>> = mutableMapOf()

            override fun getProvidingConcepts(): MutableMap<String, MutableSet<String>> = mutableMapOf()

            override fun getConstraintBucket(): ConstraintBucket = ConstraintBucket()

            override fun getGroupsBucket(): GroupsBucket = GroupsBucket()
        }

    /**
     * Get all files which are scanned by jQA for available rules.
     */
    fun getAvailableRuleSources(): List<VirtualFile> = state.availableRuleSources

    /**
     * Get the currently effective rules.
     */
    fun getEffectiveRules(): CollectRulesVisitor? = state.effectiveRules

    /**
     * Whether the maven distribution is technically supported in the current IDE setup.
     */
    fun isMavenDistributionSupported(): Boolean =
        JqaConfigFactory.Util.EXTENSION_POINT.extensions
            .any { it.handlesDistribution(JqaDistribution.MAVEN) }
}
