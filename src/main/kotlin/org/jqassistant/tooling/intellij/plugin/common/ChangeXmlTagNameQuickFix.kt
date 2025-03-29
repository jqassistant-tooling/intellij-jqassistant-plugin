package org.jqassistant.tooling.intellij.plugin.common

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle

/**
 * Quick fix that changes the tag name of the passed [XmlTag] to [newTagName].
 */
class ChangeXmlTagNameQuickFix(
    private val newTagName: String,
    tag: XmlTag,
) : LocalQuickFixOnPsiElement(tag) {
    override fun getFamilyName(): String = MessageBundle.message("change.tag")

    override fun getText(): String = MessageBundle.message("change.tag.to", newTagName)

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val tag = startElement as XmlTag
        tag.setName(newTagName)
    }
}
