package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.buschmais.jqassistant.core.rule.api.filter.RuleFilter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.AstLoadingFilter
import com.intellij.util.indexing.FileBasedIndex
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleDefinition
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingStrategy
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingStrategyFactory
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleType
import org.jqassistant.tooling.intellij.plugin.data.rules.ValueBasedJqaRuleDefinition

class JqaXmlRuleIndexingStrategy(
    private val project: Project,
) : JqaRuleIndexingStrategy {
    class Factory : JqaRuleIndexingStrategyFactory {
        override fun create(project: Project): JqaXmlRuleIndexingStrategy = JqaXmlRuleIndexingStrategy(project)
    }

    // TODO: Use a separate type based index and support the [type] filter.
    override fun getAll(type: JqaRuleType): List<JqaRuleDefinition> =
        FileBasedIndex.getInstance().getAllKeys(NameIndex.Util.NAME, project).map { name ->
            ValueBasedJqaRuleDefinition(
                name,
                type,
            )
        }

    private fun resolvePattern(identifier: String): List<JqaRuleDefinition> {
        val res = mutableListOf<JqaRuleDefinition>()

        FileBasedIndex.getInstance().processAllKeys(
            NameIndex.Util.NAME,
            { key ->
                if (RuleFilter.matches(key, identifier)) {
                    res.addAll(resolveExact(key))
                }
                true
            },
            project,
        )

        return res
    }

    private fun resolveExact(identifier: String): List<JqaRuleDefinition> {
        val res = mutableListOf<JqaRuleDefinition>()

        FileBasedIndex
            .getInstance()
            .processValues(
                NameIndex.Util.NAME,
                identifier,
                // Search in no particular file.
                null,
                { file, values ->
                    val psiManager = PsiManager.getInstance(project)
                    val psiFile = psiManager.findFile(file)
                    val psiElements =
                        AstLoadingFilter.forceAllowTreeLoading<List<PsiElement>, Throwable>(psiFile) {
                            values.mapNotNull { value ->
                                val token = psiFile?.findElementAt(value)
                                PsiTreeUtil.getParentOfType(
                                    token,
                                    XmlAttributeValue::class.java,
                                    false,
                                )
                            }
                        }

                    res.addAll(
                        psiElements.map { psiElement ->
                            ValueBasedJqaRuleDefinition(
                                identifier,
                                // TODO: Extract correct type
                                JqaRuleType.CONCEPT,
                                psiElement,
                            )
                        },
                    )
                    // Returning true signals to IntelliJ, that processing of the index should continue. Since we want
                    // to find all matching rules we need to process the whole index everytime.
                    true
                },
                // Search the whole project.
                ProjectScope.getAllScope(project),
            )
        return res
    }

    override fun resolve(identifier: String): List<JqaRuleDefinition> =
        if ("*" in identifier || "?" in identifier) {
            resolvePattern(identifier)
        } else {
            resolveExact(identifier)
        }
}
