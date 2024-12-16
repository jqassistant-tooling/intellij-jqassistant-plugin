package org.jqassistant.tooling.intellij.plugin.editor.rules.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
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
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class FindUnusedAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val manager = DomManager.getDomManager(element.project)
        val tag = element as? XmlTag?
        val domElement = manager.getDomElement(tag)

        // Check if element is rule element
        if (domElement !is RuleBase) return

        // Check if element has a reference path from main parent group
        val id : PsiElement = tag?.getAttribute("id")?.valueElement?.navigationElement ?: element
        val result = searchBaseReferenceRecursive(id, null)

        // If not referenced, add warning
        if(!result){
            holder
                .newAnnotation(
                    com.intellij.lang.annotation.HighlightSeverity.WARNING,
                    "This Ruleset has no active references, it is inactive",
                ).range(element.textRange)
                .create()
        }

        // TODO check if the rule is used in the rule set (can be referenced, but from places that are also incative)

    }
// TODO make baseElement not nullable (nullable only for development)
    private fun searchBaseReferenceRecursive(element: PsiElement, baseElement: PsiElement?) : Boolean{
        val searchScope = GlobalSearchScope.projectScope(element.project)
        val query: Query<PsiReference> = ReferencesSearch.search(element, searchScope)
        val references = mutableListOf<PsiElement>()
        query.forEach { reference ->
            val usageInfo = UsageInfo(reference)
            references.addIfNotNull(usageInfo.element)
        }

        var result = false
        if (references.isEmpty()){
            result =  false
        } else if (references.contains(baseElement)){
            result =  true
        } else {
            references.forEach { reference ->
                val parent = PsiTreeUtil.getParentOfType(reference, XmlTag::class.java)
                val parent2 = PsiTreeUtil.getParentOfType(parent, XmlTag::class.java)
                val text = parent2?.getAttributeValue("id")
                val nextSearch = parent2?.getAttribute("id")?.valueElement?.navigationElement
                if(text == "biojava:Default") {
                    result = true
                    return true
                } else if (nextSearch != null){
                    result = searchBaseReferenceRecursive(nextSearch, baseElement)
                }
            }
        }
        return result
    }

    private fun findBaseJqaGroup() : PsiElement?{
    return null
    }

}
