package org.jqassistant.tooling.intellij.plugin.editor.report

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.pom.Navigatable
import com.intellij.ui.JBSplitter
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import org.jqassistant.schema.report.v2.ConceptType
import org.jqassistant.schema.report.v2.ConstraintType
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.JqassistantReport
import org.jqassistant.schema.report.v2.ReferencableRuleType
import org.jqassistant.tooling.intellij.plugin.data.rules.JqaRuleIndexingService
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.AbstractTableModel


open class ReportToolWindowContent(
    private val project: Project,
    private val baseDir: VirtualFile,
    private val report: JqassistantReport
) {
    val contentPanel: JPanel
    private val splitter: JBSplitter

    init {
        val projectTrees = buildTreePanels()

        val firstTree = projectTrees.first()
        val scrollableTree = if (projectTrees.size == 1) {
            val scrollableTree = JBScrollPane(firstTree)

            scrollableTree
        } else {
            val subPanel = JPanel()
            for (tree in projectTrees) subPanel.add(tree)

            val scrollableTree = JBScrollPane(subPanel)
            scrollableTree
        }


        val toolWindow = SimpleToolWindowPanel(true)

        splitter = JBSplitter(false)
        splitter.firstComponent = scrollableTree

        val actionManager = ActionManager.getInstance()


        val actionGroup =
            actionManager.getAction("org.jqassistant.tooling.intellij.plugin.editor.report.actions.ReportToolbarGroup") as DefaultActionGroup
        // actionGroup.add(AboutAction())

        val actionToolbar =
            actionManager.createActionToolbar("jQAssistant Report Toolbar", actionGroup, true)
        actionToolbar.targetComponent = toolWindow
        toolWindow.toolbar = actionToolbar.component

        toolWindow.setContent(splitter)

        contentPanel = toolWindow
        // JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollableTree, JBTable())

        // Bottom panel
        // contentPanel.add(tablePanel)
    }

    private fun buildTreePanels(): List<Tree> {
        val nodeList = buildRuleTree(null, report.groupOrConceptOrConstraint)
        val cellRenderer = ReportCellRenderer()

        return nodeList.map { rootNode ->
            val treePanel = Tree(rootNode)
            TreeUIHelper.getInstance().installTreeSpeedSearch(treePanel)
            TreeUIHelper.getInstance().installSelectionSaver(treePanel)
            TreeUIHelper.getInstance().installSmartExpander(treePanel)
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
                        // buildResultTree(newNode, group.result)
                    }
                }

                else -> {

                }
            }

            currentRoot?.addChild(newNode)
        }

        return nodeList
    }

    private fun treeClickListener(event: TreeSelectionEvent) {
        val reportNode = event.path.lastPathComponent as? ReferencableRuleTypeNode ?: return

        val result = when (val rule = reportNode.ref) {
            is ConstraintType -> {
                rule.result
            }

            is ConceptType -> {
                /*
                val currentRow = reportNode.ref
                val col = currentRow.column.find { c -> c.source != null }

                if (col == null) return
                val source = col.source

                val path = "biojava-core/src/test/java${source.fileName}"
                openRelativeFileAt(path, source.startLine - 1, 0)
                 */

                rule.result
            }

            is GroupType -> {
                val ruleId = rule.id
                val ruleIndexingService = project.service<JqaRuleIndexingService>()

                getApplication().executeOnPooledThread {
                    try {
                        val navigationElement = ReadAction.compute<Navigatable?, Throwable> {
                            val definition = ruleIndexingService.resolve(ruleId) ?: return@compute null

                            val source = definition.computeSource() ?: return@compute null

                            source.navigationElement as? Navigatable
                        }

                        getApplication().invokeLater {
                            if (navigationElement == null || !navigationElement.canNavigate()) return@invokeLater

                            navigationElement.navigate(true)
                        }
                    } catch (e: Throwable) {
                        thisLogger().error("Maus", e)
                    }
                }

                null
            }

            else -> {
                null
            }
        }



        if (result == null) {
            splitter.secondComponent = null
            return
        }

        val columnNames = result.columns.column
        val rowData = result.rows.row.map { row ->
            row.column.map { col ->
                col.value
            }.toTypedArray()
        }

        val table = JBTable(
            object : AbstractTableModel() {
                override fun getColumnName(column: Int): String = columnNames[column].toString()
                override fun getRowCount(): Int = rowData.size
                override fun getColumnCount(): Int = columnNames.size
                override fun getValueAt(row: Int, col: Int): Any = rowData[row][col]
                override fun isCellEditable(row: Int, column: Int): Boolean = true

                override fun setValueAt(value: Any, row: Int, col: Int) {
                    rowData[row][col] = value.toString()
                    this.fireTableCellUpdated(row, col)
                }
            }
        )

        splitter.secondComponent = JBScrollPane(table)
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

