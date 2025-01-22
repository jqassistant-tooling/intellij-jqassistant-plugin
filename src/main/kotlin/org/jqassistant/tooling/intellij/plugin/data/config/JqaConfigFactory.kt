package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.core.resolver.configuration.ArtifactResolverConfiguration
import com.buschmais.jqassistant.core.runtime.api.configuration.Configuration
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * jQA Configuration that implements all interfaces that are required for the IntelliJ plugin to use the config
 *
 * Union type of [Configuration] and [ArtifactResolverConfiguration].
 * @see [FullArtifactConfigurationWrapper]
 */
interface FullArtifactConfiguration :
    Configuration,
    ArtifactResolverConfiguration

/**
 * Wrapper to represent an object implementing [Configuration] and [ArtifactResolverConfiguration] as
 * [FullArtifactConfiguration].
 *
 * Since [JqaConfigFactory] is registered as extension points we can't use generics there so we use
 * [FullArtifactConfiguration] to represent a config that fulfils the requirements for all jQA Apis we use.
 */
@JvmInline
value class FullArtifactConfigurationWrapper<T>(
    val config: T,
) : FullArtifactConfiguration,
    Configuration by config,
    ArtifactResolverConfiguration by config
    where T : Configuration, T : ArtifactResolverConfiguration

/**
 * Factory for creating a jQA Config.
 *
 * In order to support maven as an optional dependency the maven dependent logic needs to be injected through extension
 * points, this is why this abstraction is required.
 */
interface JqaConfigFactory {
    object Util {
        val EXTENSION_POINT =
            ExtensionPointName.create<JqaConfigFactory>("jqassistant.jqaConfigFactory")
    }

    fun handlesDistribution(distribution: JqaDistribution): Boolean

    fun assembleConfig(project: Project): FullArtifactConfiguration?
}
