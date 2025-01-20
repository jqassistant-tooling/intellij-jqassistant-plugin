package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.FilterPattern
import com.intellij.psi.util.parentsOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLScalar
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType
import org.jqassistant.tooling.intellij.plugin.editor.rules.RuleReference

class YamlPathFilter(
    vararg path: String,
) : ElementFilter {
    private val path = path.reversed()

    override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
        val yamlPsiElement: YAMLPsiElement = element as? YAMLPsiElement ?: return false

        val parents = yamlPsiElement.parentsOfType<YAMLKeyValue>(true)
        for ((key, parent) in path.asSequence() zip parents) {
            if (parent.keyText != key) return false
        }

        return true
    }

    override fun isClassAcceptable(hintClass: Class<*>?): Boolean = true
}

class PsiLanguageInjectionHostRuleReferenceProvider(
    val type: JqaRuleType? = null,
    private val soft: Boolean = true,
) : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val injectionHost = element as? PsiLanguageInjectionHost ?: return emptyArray()
        val escaper = injectionHost.createLiteralTextEscaper()
        val builder = StringBuilder()
        escaper.decode(escaper.relevantTextRange, builder)

        return arrayOf(
            RuleReference(
                injectionHost,
                builder.substring(0),
                type,
                soft,
            ),
        )
    }
}

class YamlConfigReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "groups")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.GROUP, true),
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "concepts")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.CONCEPT, true),
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "constraints")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.CONSTRAINT, true),
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "exclude-constraints")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.CONSTRAINT, true),
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "baseline", "include-constraints")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.CONSTRAINT, true),
        )

        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java).and(
                FilterPattern(YamlPathFilter("jqassistant", "analyze", "baseline", "include-concepts")),
            ),
            PsiLanguageInjectionHostRuleReferenceProvider(JqaRuleType.CONCEPT, true),
        )
    }
}
