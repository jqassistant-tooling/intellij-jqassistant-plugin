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
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.getUCallExpression
import org.jetbrains.uast.resolveToUElementOfType
import org.jetbrains.uast.wrapULiteral
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType

/**
 * Injects soft references to jQA rules for integration tests written in
 * UAST languages (e.g. Kotlin, Java).
 */
class UastItReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            injectionHostUExpression().filterWithContext { argumentExpression, processingContext ->
                val call =
                    argumentExpression.uastParent.getUCallExpression(searchLimit = 2) ?: return@filterWithContext false

                val constructorOrMethodCall = setOf(UastCallKind.CONSTRUCTOR_CALL, UastCallKind.METHOD_CALL)

                // All possible literals of the function call
                val argumentLiterals = call.valueArguments.map(::wrapULiteral)
                // The literal we are currently checking
                val searchingLiteral = wrapULiteral(argumentExpression)

                // Get the index of the current parameter we are checking
                val annotatedParameterIndex = argumentLiterals.indexOfFirst { lit -> lit == searchingLiteral }
                // This should never happen
                if (annotatedParameterIndex == -1) return@filterWithContext false

                // Tell the Reference provider which parameters can be autocompleted
                processingContext.put("annotatedParameterLiterals", argumentLiterals)
                // Tell the AnnotationPatternCondition which of
                // the parameters needs to have the annotation
                processingContext.put("annotatedParameterIndex", annotatedParameterIndex)

                // This checks if the current parameter has the annotation
                val callPattern = callExpression().with(AnnotationPatternCondition)
                callPattern.accepts(call, processingContext) &&
                    call.kind in constructorOrMethodCall
            },
            // .methodCallParameter might be more fitting
            UastItReferenceProvider,
        )
    }
}

object AnnotationPatternCondition : PatternCondition<UCallExpression>("jQARuleIdentifierAnnotationPattern") {
    override fun accepts(t: UCallExpression, context: ProcessingContext?): Boolean {
        /*
        // This is twice as fast for some reason
        val psiElement = t.sourcePsi ?: return false
        val firstChild = psiElement.firstChild
        val methodReference = firstChild.reference ?: return false
        val methodElement = methodReference.resolve()
        val uMethodElement = methodElement.toUElement(UMethod::class.java) ?: return false
         */

        // Get the UMethod element of the method that is being called
        val uMethodElement = t.resolveToUElementOfType<UMethod>() ?: return false

        // Get the index of the parameter we are currently checking
        val annotatedParameterIndex = context?.get("annotatedParameterIndex") as Int? ?: 0

        // Get the parameter we are currently checking
        val uParameter = uMethodElement.uastParameters.getOrNull(annotatedParameterIndex) ?: return false
        val uAnnotations = uParameter.uAnnotations

        // Check if any of the annotations match
        val hasAnnotation =
            uAnnotations.any { a ->
                val qualifiedName = a.qualifiedName ?: return@any false
                val annotationPackage = "com.buschmais.jqassistant.core.rule.api.annotation."

                // During index building the qualifiedName will actually be just "ConceptId"
                // If annotations should still be recognised then remove this line
                if (!qualifiedName.startsWith(annotationPackage)) return@any false

                val annotationTypeName = qualifiedName.removePrefix(annotationPackage)
                val annotationType =
                    when (annotationTypeName) {
                        "ConceptId" -> JqaRuleType.CONCEPT
                        "GroupId" -> JqaRuleType.GROUP
                        "ConstraintId" -> JqaRuleType.CONSTRAINT
                        // Unknown Annotation
                        else -> return@any false
                    }

                // Tell UastItReferenceProvider which kind of reference we are dealing with
                context?.put("jQAAnnotationType", annotationType)

                return true
            }

        return hasAnnotation
    }
}

object UastItReferenceProvider : UastReferenceProvider(listOf(UInjectionHost::class.java)) {
    override fun getReferencesByElement(element: UElement, context: ProcessingContext): Array<PsiReference> {
        /*
        val annotatedParameterLiterals = context.get("annotatedParameterLiterals") as List<UExpression>
        val annotatedParameterIndex = context.get("annotatedParameterIndex") as Int
        val matchingIndex = annotatedParameterLiterals.indexOfFirst { lit -> lit == element }
        println("size: ${annotatedParameterLiterals.size}")
        println("matchI: $matchingIndex")
        println("bool: ${matchingIndex == annotatedParameterIndex}")
         */

        val psi = element.sourcePsi ?: return emptyArray()
        val injectionHost = element as? UInjectionHost ?: return emptyArray()
        if (!injectionHost.isString) return emptyArray()
        val name = injectionHost.evaluateToString() ?: return emptyArray()

        val ruleType = context.get("jQAAnnotationType") as? JqaRuleType ?: return emptyArray()

        val ruleReference = SpecificRuleReference(psi, name, ruleType, true)

        return arrayOf(ruleReference)
    }
}
