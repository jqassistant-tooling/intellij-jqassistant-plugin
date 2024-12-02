package org.jqassistant.tooling.intellij.plugin.editor.report.inlay

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import org.jqassistant.schema.report.v2.ConceptType
import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.tooling.intellij.plugin.data.report.ReportProviderService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Concept
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Constraint

class ReportSingularRuleResultDeclarativeInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector = Collector()

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            val xmlTag = element as? XmlTag ?: return

            val domManager = DomManager.getDomManager(element.project)
            val domElement = domManager.getDomElement(xmlTag) ?: return

            val reportProviderService = element.project.service<ReportProviderService>()
            val cachedReports = reportProviderService.getCachedReports()
            val cachedReportRules =
                cachedReports.values.flatMap(ReportProviderService::flattenReport)

            val inlayText =
                when (domElement) {
                    is Concept -> {
                        val cachedResult =
                            cachedReportRules
                                .filterIsInstance<ConceptType>()
                                .find { concept -> concept.id == domElement.id.value }

                        cachedResult?.status?.toString()
                    }

                    is Constraint -> {
                        val cachedResult =
                            cachedReportRules
                                .filterIsInstance<ConstraintType>()
                                .find { constraintType -> constraintType.id == domElement.id.value }

                        cachedResult?.status?.toString()
                    }

                    else ->
                        return
                } ?: return

            sink.addPresentation(
                InlineInlayPosition(xmlTag.textRange.startOffset, false),
                hasBackground = true,
            ) {
                text(inlayText)
            }
        }
    }
}
