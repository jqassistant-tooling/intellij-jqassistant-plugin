package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.util.xml.Attribute
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomFileDescription
import com.intellij.util.xml.GenericAttributeValue
import com.intellij.util.xml.Stubbed

@Stubbed
interface JqassistantRules : DomElement {
    @get:Stubbed
    val groups: List<Group>

    @get:Stubbed
    val concepts: List<Concept>

    @get:Stubbed
    val constraints: List<Constraint>
}

interface RuleBase : DomElement {
    @get:Stubbed
    @get:Attribute("id")
    val id: GenericAttributeValue<String>
}

@Stubbed
interface Group :
    RuleBase,
    DomElement

@Stubbed
interface Concept :
    RuleBase,
    DomElement

@Stubbed
interface Constraint :
    RuleBase,
    DomElement

class JqaXmlRuleDescription :
    DomFileDescription<JqassistantRules>(JqassistantRules::class.java, "jqassistant-rules")
