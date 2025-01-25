package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.lang.Language
import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.CypherType

class XmlRuleCypherEmbedding : LanguageInjectionContributor {
    override fun getInjection(host: PsiElement): Injection? {
        val xmlPsi = host as? XmlText ?: return null
        val lang = Language.findLanguageByID("Cypher") ?: return null

        val domManager = DomManager.getDomManager(host.project)
        val domElement =
            domManager.getDomElement(PsiTreeUtil.getParentOfType(xmlPsi, XmlTag::class.java)) ?: return null
        if (domElement !is CypherType) return null

        return SimpleInjection(lang, "", "", null)
    }
}
