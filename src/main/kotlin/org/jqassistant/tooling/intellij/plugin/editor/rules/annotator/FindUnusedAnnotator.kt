package org.jqassistant.tooling.intellij.plugin.editor.rules.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.FileBasedIndex
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.NameIndex
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class FindUnusedAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Check if element is rule element
        if (element !is RuleBase) return
        // Check if element has a reference path from main parent group

        // If rule but not referenced, add warning
        val textRange = element.textRange
        holder
            .newAnnotation(
                com.intellij.lang.annotation.HighlightSeverity.WARNING,
                "This is a test warning",
            ).range(textRange)
            .create()
    }

    private fun findAllChildrenRecursive(element: PsiElement, bucket: List<PsiElement>): List<PsiElement> {
        val children = mutableListOf<PsiElement>()
        element.children.forEach {
            children.add(it)
            children.addAll(findAllChildren(it))
        }
        return children
    }

    private fun findParentGroup(elements: List<PsiElement>): XmlTag? {
        var result: XmlTag? = null
        elements.filter { it as? XmlTag? != null && it is RuleBase }.forEach {
            if ((it as XmlTag).getAttributeValue("id") == "biojava:Default") {
                result = it
                return result
            }
        }
        return result
    }

    private fun findAllChildren(element: PsiElement): List<PsiElement> {
        val psiFiles = findEligiblePsiFiles(element)
        val bucket = mutableListOf(element)
        if (element is XmlTag) {
            val includesConcepts = element.findSubTags("includesConcept")
            for (includesConcept in includesConcepts) {
                val refId = includesConcept.getAttributeValue("refId")
                // get the original concept
            }

            val includesConstraints = element.findSubTags("includesConstraint")
            val includesGroup = element.findSubTags("includesGroup")
        }
        return bucket
    }

    private fun findEligiblePsiFiles(element: PsiElement): List<PsiElement> {
        val fileIndex =
            FileBasedIndex
                .getInstance()

        val keys = fileIndex.getAllKeys(NameIndex.Util.NAME, element.project)
        val files = mutableSetOf<VirtualFile>()

        for (key in keys) {
            val keyFiles =
                fileIndex
                    .getContainingFilesIterator(
                        NameIndex.Util.NAME,
                        key,
                        GlobalSearchScope.projectScope(element.project),
                    )
            for (file in keyFiles) files.add(file)
        }
        return files.mapNotNull { it.findPsiFile(element.project)?.containingFile }
    }
}
