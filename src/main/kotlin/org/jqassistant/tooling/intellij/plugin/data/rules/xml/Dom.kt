package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.util.xml.Attribute
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomFileDescription
import com.intellij.util.xml.GenericAttributeValue
import com.intellij.util.xml.NameValue
import com.intellij.util.xml.Referencing
import com.intellij.util.xml.Stubbed
import com.intellij.util.xml.SubTagList
import org.jqassistant.tooling.intellij.plugin.editor.rules.XmlRuleReferenceConverter

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
    @get:NameValue(referencable = true)
    val id: GenericAttributeValue<String>
}

@Stubbed
interface Group :
    RuleBase,
    DomElement {
    @get:SubTagList("includeConcept")
    val includeConcept: List<IncludedConceptType>

    @get:SubTagList("includeConstraint")
    val includeConstraint: List<IncludedReferenceType>

    @get:SubTagList("includeGroup")
    val includeGroup: List<IncludedReferenceType>
}

@Stubbed
interface Concept :
    RuleBase,
    DomElement,
    ExecutableRuleType {
    @get:SubTagList("providesConcept")
    val providesConcept: List<ReferenceType>
}

@Stubbed
interface Constraint :
    RuleBase,
    ExecutableRuleType,
    DomElement

interface ExecutableRuleType : DomElement {
    @get:SubTagList("requiresConcept")
    val requiresConcept: List<OptionalReferenceType>
}

interface ReferenceType : DomElement {
    @get:Attribute("refId")
    @get:Referencing(XmlRuleReferenceConverter::class, soft = false)
    val refType: GenericAttributeValue<String>
}

interface OptionalReferenceType :
    DomElement,
    ReferenceType {
    @get:Attribute("optional")
    val optional: GenericAttributeValue<Boolean>
}

interface IncludedReferenceType :
    DomElement,
    ReferenceType {
    @get:Attribute("severity")
    val severity: GenericAttributeValue<String>
}

interface IncludedConceptType :
    DomElement,
    IncludedReferenceType {
    @get:SubTagList("providesConcept")
    val providesConcept: List<ReferenceType>
}

class JqaXmlRuleDescription :
    DomFileDescription<JqassistantRules>(JqassistantRules::class.java, "jqassistant-rules")
