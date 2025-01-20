package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.buschmais.jqassistant.core.rule.api.annotation.ConceptId
import com.buschmais.jqassistant.core.rule.api.annotation.ConstraintId
import com.buschmais.jqassistant.core.rule.api.annotation.GroupId
import com.intellij.codeInspection.restriction.AnnotationContext
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.UastInjectionHostReferenceProvider
import com.intellij.psi.registerUastReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.expressions.UInjectionHost
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

/**
 * Injects a soft [PsiReference] to jQA rules for method call parameters
 * that have a rule identifier annotation (e.g. [ConceptId]) in UAST
 * languages (e.g. Kotlin, Java). These are often found in integration tests
 */
class UastReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression(),
            UastReferenceProvider,
        )
    }
}

/**
 * Resolves the correct [PsiReference] for a given [UExpression] if it is annotated with an jQA id annotation.
 */
object UastReferenceProvider : UastInjectionHostReferenceProvider() {
    override fun getReferencesForInjectionHost(
        uExpression: UExpression,
        host: PsiLanguageInjectionHost,
        context: ProcessingContext,
    ): Array<PsiReference> {
        val uHost = uExpression as? UInjectionHost ?: return emptyArray()
        val stringLiteralContent = uHost.evaluateToString() ?: return emptyArray()

        val annotationContext = AnnotationContext.fromExpression(uHost)

        for (owner in annotationContext.allItems()) {
            if (owner.hasAnnotation(GroupId::class.qualifiedName!!)) {
                return arrayOf(
                    RuleReference(host, stringLiteralContent, JqaRuleType.GROUP, true),
                )
            }
            if (owner.hasAnnotation(ConceptId::class.qualifiedName!!)) {
                return arrayOf(
                    RuleReference(host, stringLiteralContent, JqaRuleType.CONCEPT, true),
                )
            }
            if (owner.hasAnnotation(ConstraintId::class.qualifiedName!!)) {
                return arrayOf(
                    RuleReference(host, stringLiteralContent, JqaRuleType.CONSTRAINT, true),
                )
            }
        }

        return emptyArray()
    }
}
