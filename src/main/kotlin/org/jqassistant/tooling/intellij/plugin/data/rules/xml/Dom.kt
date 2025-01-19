package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.util.xml.Attribute
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomFileDescription
import com.intellij.util.xml.GenericAttributeValue
import com.intellij.util.xml.NameValue
import com.intellij.util.xml.Referencing
import com.intellij.util.xml.Stubbed
import com.intellij.util.xml.SubTagList
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType
import org.jqassistant.tooling.intellij.plugin.editor.rules.XmlConceptReferenceConverter
import org.jqassistant.tooling.intellij.plugin.editor.rules.XmlConstraintReferenceConverter
import org.jqassistant.tooling.intellij.plugin.editor.rules.XmlGroupReferenceConverter

@Stubbed
interface JqassistantRules : DomElement {
    @get:Stubbed
    val groups: List<Group>

    @get:Stubbed
    val concepts: List<Concept>

    @get:Stubbed
    val constraints: List<Constraint>
}

sealed interface RuleBase : DomElement {
    @get:Stubbed
    @get:Attribute("id")
    @get:NameValue(referencable = true)
    val id: GenericAttributeValue<String>
}

// Needs to be an extension function since IntelliJ does some magic with the interfaces.
fun RuleBase.getType(): JqaRuleType =
    when (this) {
        is Group -> JqaRuleType.GROUP
        is Concept -> JqaRuleType.CONCEPT
        is Constraint -> JqaRuleType.CONSTRAINT
    }

@Stubbed
interface Group :
    RuleBase,
    DomElement {
    @get:SubTagList("includeConcept")
    val includeConcept: List<IncludedConceptType>

    @get:SubTagList("includeConstraint")
    val includeConstraint: List<ConstraintType>

    @get:SubTagList("includeGroup")
    val includeGroup: List<GroupType>
}

@Stubbed
interface Concept :
    RuleBase,
    ExecutableRuleType,
    DomElement {
    @get:SubTagList("providesConcept")
    val providesConcept: List<ConceptType>
}

@Stubbed
interface Constraint :
    RuleBase,
    ExecutableRuleType,
    DomElement

interface ExecutableRuleType : DomElement {
    @get:SubTagList("requiresConcept")
    val requiresConcept: List<ConceptType>
}

interface GroupType : DomElement {
    @get:Attribute("refId")
    @get:Referencing(XmlGroupReferenceConverter::class, soft = false)
    val refType: GenericAttributeValue<String>
}

interface ConstraintType : DomElement {
    @get:Attribute("refId")
    @get:Referencing(XmlConstraintReferenceConverter::class, soft = false)
    val refType: GenericAttributeValue<String>
}

interface ConceptType : DomElement {
    @get:Attribute("refId")
    @get:Referencing(XmlConceptReferenceConverter::class, soft = false)
    val refType: GenericAttributeValue<String>
}

interface IncludedConceptType :
    DomElement,
    ConceptType {
    @get:SubTagList("providesConcept")
    val providesConcept: List<ConceptType>
}

class JqaXmlRuleDescription :
    DomFileDescription<JqassistantRules>(JqassistantRules::class.java, "jqassistant-rules")
