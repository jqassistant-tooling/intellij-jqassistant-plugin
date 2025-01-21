package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.AstLoadingFilter
import com.intellij.util.indexing.FileBasedIndex
import org.jqassistant.tooling.intellij.plugin.common.WildcardUtil
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

    override fun getAll(type: JqaRuleType?): List<JqaRuleDefinition> {
        val res = mutableListOf<JqaRuleDefinition>()

        FileBasedIndex.getInstance().processAllKeys(
            NameIndex.Util.NAME,
            { ruleId ->
                val typeSet = mutableSetOf<JqaRuleType>()
                FileBasedIndex.getInstance().processValues(
                    NameIndex.Util.NAME,
                    ruleId,
                    null,
                    { _, declarations ->
                        for (declaration in declarations) {
                            typeSet.add(declaration.type)
                        }

                        // If the ruleId has more than one type (user error, but we handle it) and the filter type is one
                        // of them, we don't need to continue since the ruleId will be included and the type will be `null`
                        // in all cases.
                        !(typeSet.size > 1 && (type == null || type in typeSet))
                    },
                    ProjectScope.getAllScope(project),
                )

                if (type == null || type in typeSet) {
                    res.add(
                        ValueBasedJqaRuleDefinition(
                            ruleId,
                            // If we don't know the type for sure we leave it empty.
                            if (typeSet.size == 1) typeSet.single() else null,
                        ),
                    )
                }

                // Searching all keys so we need to continue.
                true
            },
            project,
        )

        return res
    }

    private fun resolvePattern(identifier: String, type: JqaRuleType?): List<JqaRuleDefinition> {
        val res = mutableListOf<JqaRuleDefinition>()

        FileBasedIndex.getInstance().processAllKeys(
            NameIndex.Util.NAME,
            { key ->
                if (WildcardUtil.matches(key, identifier)) {
                    res.addAll(resolveExact(key, type))
                }
                true
            },
            project,
        )

        return res
    }

    private fun resolveExact(identifier: String, filterType: JqaRuleType?): List<JqaRuleDefinition> {
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
                    val references =
                        AstLoadingFilter.forceAllowTreeLoading<List<Pair<PsiElement, JqaRuleType>>, Throwable>(
                            psiFile,
                        ) {
                            values.mapNotNull { (offset, type) ->
                                if (filterType != null && filterType != type) {
                                    null
                                } else {
                                    val token = psiFile?.findElementAt(offset)
                                    PsiTreeUtil
                                        .getParentOfType(
                                            token,
                                            XmlAttributeValue::class.java,
                                            false,
                                        )?.let { Pair(it, type) }
                                }
                            }
                        }

                    res.addAll(
                        references.map { (psiElement, type) ->
                            ValueBasedJqaRuleDefinition(
                                identifier,
                                type,
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

    override fun resolve(identifier: String, type: JqaRuleType?): List<JqaRuleDefinition> =
        if (WildcardUtil.looksLikeWildcard(identifier)) {
            resolvePattern(identifier, type)
        } else {
            resolveExact(identifier, type)
        }
}
