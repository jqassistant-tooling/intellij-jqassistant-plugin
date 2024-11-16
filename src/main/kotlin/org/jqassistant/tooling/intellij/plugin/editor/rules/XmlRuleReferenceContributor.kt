package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext

class XmlRuleReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // TODO: Adjust pattern to only match refId attributes at the right structural place, and decide possible types from context.
        registrar.registerReferenceProvider(
            psiElement().inside(XmlPatterns.xmlFile()).inside(XmlPatterns.xmlAttributeValue("refId")),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val value = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java, false)
                    if (value == null) {
                        thisLogger().warn("Reference is not a `XmlAttributeValue`")
                    }
                    return arrayOf(RuleReference(element, value?.value ?: element.text))
                }
            }
        )
    }
}
