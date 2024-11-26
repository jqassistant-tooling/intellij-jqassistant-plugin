package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion

class YamlConfigSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> =
        mutableListOf(YamlConfigSchemaProvider())
}

class YamlConfigSchemaProvider : JsonSchemaFileProvider {
    companion object {
        // TODO: Replace with full schema from jQA schemata package.
        const val SCHEMA_PATH = "/schemata/config.schema.json"
        val SCHEMA_VERSION = JsonSchemaVersion.SCHEMA_7
    }

    // TODO: Use resource bundle.
    override fun getName() = "jQAssistant config"

    override fun isAvailable(file: VirtualFile) = ConfigFileUtils.isJqaConfigFile(file)

    override fun getSchemaVersion() = SCHEMA_VERSION

    override fun getSchemaType() = SchemaType.schema

    override fun getSchemaFile() =
        JsonSchemaProviderFactory.getResourceFile(YamlConfigSchemaProvider::class.java, SCHEMA_PATH)
}
