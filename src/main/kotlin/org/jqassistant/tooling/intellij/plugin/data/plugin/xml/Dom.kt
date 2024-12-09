package org.jqassistant.tooling.intellij.plugin.data.plugin.xml

import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomFileDescription
import com.intellij.util.xml.TagValue

interface JqassistantPlugin : DomElement {
    val rules: List<PluginRules>
}

interface PluginRules : DomElement {
    val resources: List<Resource>
}

interface Resource : DomElement {
    @get:TagValue
    val value: String
}

class JqaXmlPluginDescription :
    DomFileDescription<JqassistantPlugin>(JqassistantPlugin::class.java, "jqassistant-plugin")
