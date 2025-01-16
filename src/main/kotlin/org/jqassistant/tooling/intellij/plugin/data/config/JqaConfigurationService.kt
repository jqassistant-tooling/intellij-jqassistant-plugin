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
import java.util.Optional

enum class JqaDistribution {
    CLI,
    MAVEN,
}

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

        return factory.createConfig(project) as CliConfiguration
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
