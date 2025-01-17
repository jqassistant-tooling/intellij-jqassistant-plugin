package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.buschmais.jqassistant.core.rule.api.annotation.ConceptId
import com.buschmais.jqassistant.core.rule.api.annotation.ConstraintId
import com.buschmais.jqassistant.core.rule.api.annotation.GroupId
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
import com.intellij.openapi.util.Key
import com.intellij.patterns.uast.UExpressionPattern

/*
 * The index of the parameter with the correct annotation in the
 * current method call
 */
val annotatedParameterIndexKey = Key.create<Int>("annotatedParameterIndex")

/**
 * Injects a soft [PsiReference] to jQA rules for method call parameters
 * that have a rule identifier annotation (e.g. @ConceptId) in UAST
 * languages (e.g. Kotlin, Java). These are often found in integration tests
 */
class UastReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerUastReferenceProvider(
            annotatedParameterPattern(),
            UastReferenceProvider,
        )
    }

    private fun annotatedParameterPattern(): UExpressionPattern<*, *> {
        return injectionHostUExpression().filterWithContext { argumentExpression, processingContext ->
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

            // Tell the AnnotationPatternCondition which of
            // the parameters needs to have the annotation
            processingContext.put(annotatedParameterIndexKey, annotatedParameterIndex)

            // This checks if the current parameter has the annotation
            val callPattern = callExpression().with(AnnotationPatternCondition)
            callPattern.accepts(call, processingContext) &&
                call.kind in constructorOrMethodCall
        }
    }
}

/**
 * Checks if a method call contains a correctly annotated parameter at the `annotatedParameterIndex`
 * that is set in the current [ProcessingContext]
 */
object AnnotationPatternCondition : PatternCondition<UCallExpression>("jQARuleIdentifierAnnotationPattern") {
    override fun accepts(t: UCallExpression, context: ProcessingContext?): Boolean {
        // Get the UMethod element of the method that is being called
        val uMethodElement = t.resolveToUElementOfType<UMethod>() ?: return false

        // Get the index of the parameter we are currently checking
        val annotatedParameterIndex = context?.get(annotatedParameterIndexKey) ?: 0

        // Get the parameter we are currently checking
        val uParameter = uMethodElement.uastParameters.getOrNull(annotatedParameterIndex) ?: return false
        val uAnnotations = uParameter.uAnnotations

        // Check if any of the annotations match
        val hasAnnotation =
            uAnnotations.any { a ->
                val qualifiedName = a.qualifiedName ?: return@any false

                // During index building the `qualifiedName` will actually be just "ConceptId"
                // instead of the actual qualified name of the Annotation. Because of this
                // the feature only works after indexing is done
                val annotationType =
                    when (qualifiedName) {
                        ConceptId::class.qualifiedName -> JqaRuleType.CONCEPT
                        GroupId::class.qualifiedName -> JqaRuleType.GROUP
                        ConstraintId::class.qualifiedName -> JqaRuleType.CONSTRAINT
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

/**
 * Resolves the correct [PsiReference] for a given [UElement] (in this case a method call parameter)
 * with the help of the `jQAAnnotationType` attribute set in the current [ProcessingContext]
 */
object UastReferenceProvider : UastReferenceProvider(listOf(UInjectionHost::class.java)) {
    override fun getReferencesByElement(element: UElement, context: ProcessingContext): Array<PsiReference> {
        // The string literal for which we want to suggest rules
        val psiElement = element.sourcePsi ?: return emptyArray()
        val injectionHost = element as? UInjectionHost ?: return emptyArray()
        if (!injectionHost.isString) return emptyArray()

        // The text already typed by the user
        // Note: this does not affect the output of `ruleReference.getVariants()`, the suggestions
        // are filtered by IntelliJ itself
        val stringLiteralContent = injectionHost.evaluateToString() ?: return emptyArray()

        // Get which type of annotation this parameter has
        val annotatedRuleType = context.get("jQAAnnotationType") as? JqaRuleType ?: return emptyArray()

        // A rule reference to all rules of this rule type
        val ruleReference = SpecificRuleReference(psiElement, stringLiteralContent, annotatedRuleType, true)

        return arrayOf(ruleReference)
    }
}

