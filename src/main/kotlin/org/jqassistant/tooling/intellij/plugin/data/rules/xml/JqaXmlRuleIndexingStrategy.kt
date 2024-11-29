package org.jqassistant.tooling.intellij.plugin.data.rules.xml

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
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

    override fun resolve(name: String): JqaRuleDefinition? {
        val res: Ref<JqaRuleDefinition?> = Ref.create()
        FileBasedIndex
            .getInstance()
            .processValues(
                NameIndex.Util.NAME,
                name,
                null,
                { file, value ->
                    val psiManager = PsiManager.getInstance(project)
                    val psiFile = psiManager.findFile(file)
                    val psiElement =
                        AstLoadingFilter.forceAllowTreeLoading<PsiElement?, Throwable>(psiFile) {
                            val token = psiFile?.findElementAt(value)
                            return@forceAllowTreeLoading PsiTreeUtil.getParentOfType(
                                token,
                                XmlAttributeValue::class.java,
                                false,
                            )
                        }

                    if (psiElement == null) return@processValues true

                    res.set(
                        ValueBasedJqaRuleDefinition(
                            name,
                            // TODO: Extract correct type
                            JqaRuleType.CONCEPT,
                            psiElement,
                        ),
                    )
                    false
                },
                GlobalSearchScope.projectScope(project),
            )
        return res.get()
    }
}
