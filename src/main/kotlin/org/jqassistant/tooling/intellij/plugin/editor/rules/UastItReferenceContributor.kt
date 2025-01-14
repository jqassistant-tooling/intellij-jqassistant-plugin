package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.uast.callExpression
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.UastReferenceProvider
import com.intellij.psi.registerUastReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.toUElement

/**
 * Injects soft references to jQA rules for integration tests written in
 * UAST languages (e.g. Kotlin, Java).
 */
class UastItReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression()
                .callParameter(
                    0,
                    callExpression()
                        .with(AnnotationPatternCondition),
                ),
            UastItReferenceProvider,
        )
    }
}

object AnnotationPatternCondition : PatternCondition<UCallExpression>("jQARuleIdentifierAnnotationPattern") {
    override fun accepts(t: UCallExpression, context: ProcessingContext?): Boolean {
        val psiElm = t.sourcePsi ?: return false
        val firstChild = psiElm.firstChild
        val methodReference = firstChild.reference ?: return false
        val methodElement = methodReference.resolve()
        val uMethodElement = methodElement.toUElement(UMethod::class.java) ?: return false
        val uParameter = uMethodElement.uastParameters.firstOrNull() ?: return false
        val uAnnotations = uParameter.uAnnotations
        for (a in uAnnotations) {
            println(a.qualifiedName)
        }

        val hasAnnotation =
            uAnnotations.any { a ->
                val qualifiedName = a.qualifiedName ?: return@any false
                val annotationPackage = "com.buschmais.jqassistant.core.rule.api.annotation."

                // During index building the qualifiedName will actually be just "ConceptId"
                // If annotations should still be recognised then remove this line
                if (!qualifiedName.startsWith(annotationPackage)) return@any false

                val annotationType = qualifiedName.removePrefix(annotationPackage)

                arrayOf("ConceptId", "GroupId", "ConstraintId").contains(annotationType)
            }

        return hasAnnotation
    }
}

object UastItReferenceProvider : UastReferenceProvider(listOf(UInjectionHost::class.java)) {
    override fun getReferencesByElement(element: UElement, context: ProcessingContext): Array<PsiReference> {
        val psi = element.sourcePsi ?: return emptyArray()
        // println(element)
        val injectionHost = element as? UInjectionHost ?: return emptyArray()
        if (!injectionHost.isString) return emptyArray()
        val name = injectionHost.evaluateToString() ?: return emptyArray()
        return arrayOf(
            RuleReference(psi, name, true),
        )
    }
}
