package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.xml.ConvertContext
import com.intellij.util.xml.CustomReferenceConverter
import com.intellij.util.xml.GenericDomValue
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

open class XmlRuleReferenceConverter(
    private val type: JqaRuleType? = null,
) : CustomReferenceConverter<String> {
    override fun createReferences(
        value: GenericDomValue<String>?,
        element: PsiElement?,
        context: ConvertContext?,
    ): Array<PsiReference> {
        if (element == null) return emptyArray()
        return arrayOf(RuleReference(element, value?.value ?: element.text, type))
    }
}

class XmlGroupReferenceConverter : XmlRuleReferenceConverter(JqaRuleType.GROUP)

class XmlConceptReferenceConverter : XmlRuleReferenceConverter(JqaRuleType.CONCEPT)

class XmlConstraintReferenceConverter : XmlRuleReferenceConverter(JqaRuleType.CONSTRAINT)
