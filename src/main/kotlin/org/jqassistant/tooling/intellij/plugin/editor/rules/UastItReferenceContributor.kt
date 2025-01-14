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
        val patternCondition =
            object : PatternCondition<UCallExpression>("as") {
                override fun accepts(t: UCallExpression, context: ProcessingContext?): Boolean {
                    val psiElm = t.sourcePsi ?: return false
                    val firstChild = psiElm.firstChild
                    val refElm = firstChild.reference ?: return false
                    val resolved = refElm.resolve()
                    val uElement = resolved.toUElement(UMethod::class.java) ?: return false
                    val parameter = uElement.uastParameters.firstOrNull() ?: return false
                    val annotations = parameter.uAnnotations
                    val hasAnnotation = annotations.any { a -> a.qualifiedName == "ConceptId" }

                    // val arg = t.valueArguments.firstOrNull() ?: return false
                    // val annos = arg.uAnnotations

                    return hasAnnotation
                }
            }

        registrar.registerUastReferenceProvider(
            injectionHostUExpression()
                .callParameter(
                    0,
                    callExpression()
                        .withMethodNames(
                            listOf(
                                "applyConcept",
                                "executeGroup",
                            ),
                        ).with(patternCondition), // .withReceiver(uClass("com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT")),
                ),
            UastItReferenceProvider,
        )
    }
}

object UastItReferenceProvider : UastReferenceProvider(listOf(UInjectionHost::class.java)) {
    override fun getReferencesByElement(element: UElement, context: ProcessingContext): Array<PsiReference> {
        val psi = element.sourcePsi ?: return emptyArray()
        println(element)
        val injectionHost = element as? UInjectionHost ?: return emptyArray()
        if (!injectionHost.isString) return emptyArray()
        val name = injectionHost.evaluateToString() ?: return emptyArray()
        return arrayOf(
            RuleReference(psi, name, true),
        )
    }
}
