package org.jqassistant.tooling.intellij.plugin.editor.report

import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.jqassistant.schema.report.v2.*
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent


open class ReportToolWindowContent(
    private val project: Project,
    private val baseDir: VirtualFile,
    private val report: JqassistantReport
) {
    val contentPanel: JBScrollPane

    init {
        val projectTrees = treePanel()

        val firstTree = projectTrees.first()
        if (projectTrees.size == 1) {
            contentPanel = JBScrollPane(firstTree)
        } else {
            val subPanel = JPanel()
            for (tree in projectTrees) subPanel.add(tree)

            contentPanel = JBScrollPane(subPanel)
        }
    }

    private fun treePanel(): List<Tree> {
        val nodeList = buildRuleTree(null, report.groupOrConceptOrConstraint)
        val cellRenderer = ReportCellRenderer()

        return nodeList.map { rootNode ->
            val treePanel = Tree(rootNode)
            treePanel.cellRenderer = cellRenderer

            treePanel.addTreeSelectionListener { event -> treeClickListener(event) }
            treePanel
        }
    }

    private fun buildRuleTree(
        currentRoot: ReportNode?,
        currentReport: List<ReferencableRuleType>
    ): List<ReportNode> {
        val nodeList = mutableListOf<ReportNode>();
        for (group in currentReport) {
            val newNode = ReferencableRuleTypeNode(group, currentRoot)
            nodeList.add(newNode)

            when (group) {
                is GroupType -> {
                    buildRuleTree(newNode, group.groupOrConceptOrConstraint)
                }

                is ConstraintType -> {
                }

                is ConceptType -> {
                    val result = group.result
                    if (result != null) {
                        buildResultTree(newNode, group.result.rows.row)
                    }
                }

                else -> {

                }
            }

            currentRoot?.addChild(newNode)
        }

        return nodeList
    }

    private fun buildResultTree(
        currentRoot: ReferencableRuleTypeNode,
        currentResult: List<RowType>
    ): List<ReportNode> {
        val nodeList = mutableListOf<ReportNode>();
        for (resultRow in currentResult) {
            val newNode = ConstraintResultRowNode(resultRow, currentRoot)
            nodeList.add(newNode)

            currentRoot.addChild(newNode)
        }

        return nodeList
    }

    private fun treeClickListener(event: TreeSelectionEvent) {
        when (val reportNode = event.path.lastPathComponent as ReportNode) {
            is ConstraintResultRowNode -> {
                val currentRow = reportNode.ref
                val col = currentRow.column.find { c -> c.source != null }

                if (col == null) return
                val source = col.source

                val path = "biojava-core/src/test/java${source.fileName}"
                openRelativeFileAt(path, source.startLine - 1, 0)
            }
        }
    }

    private fun openRelativeFileAt(relativePath: String, line: Int, column: Int) {
        val file = baseDir.findFile(relativePath)

        if (file != null) {
            val manager = FileEditorManager.getInstance(project)
            val descriptor = OpenFileDescriptor(project, file, line, column)

            getApplication().invokeLater({
                manager.openTextEditor(descriptor, true)
            }, ModalityState.defaultModalityState())
        }
    }
}
