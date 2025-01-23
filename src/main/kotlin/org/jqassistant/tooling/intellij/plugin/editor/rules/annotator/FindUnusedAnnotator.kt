package org.jqassistant.tooling.intellij.plugin.editor.rules.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.usageView.UsageInfo
import com.intellij.util.Query
import com.intellij.util.containers.addIfNotNull
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

class FindUnusedAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val result: Boolean
        val project = element.project
        val manager = DomManager.getDomManager(project)
        val tag = element as? XmlTag?
        val domElement = manager.getDomElement(tag)
        if (domElement !is RuleBase) return
        val idValue = tag?.getAttributeValue("id")

        // Compare with ruleSet
        val configService = element.project.service<JqaConfigurationService>()
        val ruleSet = configService.getEffectiveRules()
        val allIDs =
            mutableListOf<String>().apply {
                addAll(ruleSet?.groups?.map { it.id } ?: emptyList())
                addAll(ruleSet?.concepts?.map { it.key.id } ?: emptyList())
                addAll(ruleSet?.constraints?.map { it.key.id } ?: emptyList())
            }
        result = allIDs.contains(idValue)

        if (!result) {
            holder
                .newAnnotation(
                    com.intellij.lang.annotation.HighlightSeverity.WARNING,
                    MessageBundle.message("annotator.inactive.rule"),
                ).range(element)
                .create()
        }
    }

    private fun searchBaseReferenceRecursive(element: PsiElement, baseGroup: String): Boolean {
        val searchScope = GlobalSearchScope.projectScope(element.project)
        val query: Query<PsiReference> = ReferencesSearch.search(element, searchScope)
        val references = mutableListOf<PsiElement>()
        query.forEach { reference ->
            val usageInfo = UsageInfo(reference)
            references.addIfNotNull(usageInfo.element)
        }

        var result = false
        if (references.isEmpty()) {
            result = false
        } else {
            for (reference in references) {
                val refIdTag = PsiTreeUtil.getParentOfType(reference, XmlTag::class.java)
                val ruleTag = PsiTreeUtil.getParentOfType(refIdTag, XmlTag::class.java)
                val text = ruleTag?.getAttributeValue("id")
                val nextSearch = ruleTag?.getAttribute("id")?.valueElement?.navigationElement
                if (text == baseGroup) {
                    result = true
                    break
                } else if (nextSearch != null) {
                    result = result || searchBaseReferenceRecursive(nextSearch, baseGroup)
                }
            }
        }
        return result
    }
}
