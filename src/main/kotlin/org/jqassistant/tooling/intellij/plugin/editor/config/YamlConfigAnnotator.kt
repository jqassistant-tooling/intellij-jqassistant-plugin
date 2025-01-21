package org.jqassistant.tooling.intellij.plugin.editor.config

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jqassistant.tooling.intellij.plugin.common.WildcardUtil
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import org.jqassistant.tooling.intellij.plugin.editor.rules.RuleReference

/**
 * Annotates invalid [RuleReference]s in yaml files in order to get proper highlighting for them.
 */
class YamlConfigAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        for (reference in element.references) {
            if (reference is RuleReference) {
                val results = reference.multiResolve(false)
                if (WildcardUtil.looksLikeWildcard(reference.name)) {
                    if (results.isEmpty()) {
                        holder
                            .newAnnotation(
                                HighlightSeverity.WARNING,
                                MessageBundle.message("wildcard.matches.nothing"),
                            ).range(element)
                            .create()
                    }
                } else {
                    if (results.none { it.isValidResult }) {
                        holder
                            .newAnnotation(
                                HighlightSeverity.ERROR,
                                MessageBundle.message("cannot.resolve", reference.name),
                            ).range(element)
                            .create()
                    }
                }
            }
        }
    }
}
