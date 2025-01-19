package org.jqassistant.tooling.intellij.plugin.editor.rules

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.util.xml.DomManager
import org.jqassistant.tooling.intellij.plugin.data.config.JqaConfigurationService
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.JqassistantRules
import org.jqassistant.tooling.intellij.plugin.editor.MessageBundle
import java.util.function.Function
import javax.swing.JComponent

/**
 * Displays a warning when editing an xml rule file that is not active as per the jqa config.
 */
class InactiveXmlRuleSourceNotificationProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file in
            project
                .service<JqaConfigurationService>()
                .getAvailableRuleSources() ||
            !file.name.endsWith(".xml")
        ) {
            return null
        }

        val xmlFile = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: return null
        DomManager.getDomManager(project).getFileElement(xmlFile, JqassistantRules::class.java) ?: return null

        return Function { fileEditor ->
            val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning)
            val message = MessageBundle.message("inactive.rule.source.warning")

            panel.text(message)

            panel
        }
    }
}
