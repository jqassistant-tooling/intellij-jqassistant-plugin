package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.codeInspection.actions.CodeInspectionAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

class SynchronizeListener(
    private val project: Project,
) : JqaSyncListener {
    override fun synchronize(config: FullArtifactConfiguration?) {
        ApplicationManager.getApplication().invokeLater {
            ActionManager.getInstance().tryToExecute(CodeInspectionAction(), null, null, null, false)
        }
        /*
                ApplicationManager.getApplication().invokeLater {
                    val inspectionManager = InspectionManager.getInstance(project)
                    val profile = InspectionProfileManager.getInstance(project).currentProfile
                    val tools = profile.getAllEnabledInspectionTools(project).map { it.tool }
                    val context = inspectionManager.createNewGlobalContext()
                    inspectionManager.ins(InspectionScope(psiFile), tools, context)
                }

        val inspectionManager = InspectionManager.getInstance(project)
        val globalContext = inspectionManager.createNewGlobalContext()
        val file =
            FileEditorManager
                .getInstance(project)
                .selectedFiles
                .firstOrNull()
                ?.findPsiFile(project)
                ?: return
        val wrapper = LocalInspectionToolWrapper(XmlRuleDomInspection())
        InspectionEngine.runInspectionOnFile(file, wrapper, globalContext)*/
    }
}
