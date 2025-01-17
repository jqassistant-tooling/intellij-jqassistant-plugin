package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.core.runtime.api.configuration.Configuration
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

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

    fun createConfig(project: Project): Configuration?
}
