package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.xml.ConvertContext
import com.intellij.util.xml.CustomReferenceConverter
import com.intellij.util.xml.GenericDomValue

class XmlRuleReferenceConverter : CustomReferenceConverter<String> {
    override fun createReferences(
        value: GenericDomValue<String>?,
        element: PsiElement?,
        context: ConvertContext?,
    ): Array<PsiReference> {
        if (element == null) return emptyArray()
        return arrayOf(RuleReference(element, value?.value ?: element.text))
    }
}
