package org.jqassistant.tooling.intellij.plugin.editor.rules.refactoring.delete

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.NameIndex

/**
 * Somehow this is never called but intellij still calls the safe delete processor - delete this file in final version
 */
class RuleFileRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isAvailable(context: PsiElement): Boolean {
        println("called file supportprovider")
        return findEligiblePsiFiles(context).contains(context)
    }

    /**
     * Find all eligible psiFiles in the project (jqa rule files)
     * TODO this operation belongs into a the JqaRuleIndexingService
     * @param element the element that is being deleted
     */
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
