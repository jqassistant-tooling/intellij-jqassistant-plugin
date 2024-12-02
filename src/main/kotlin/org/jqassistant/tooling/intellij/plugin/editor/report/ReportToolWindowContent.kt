package org.jqassistant.tooling.intellij.plugin.editor.report

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.pom.Navigatable
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TableSpeedSearch
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
import org.jqassistant.tooling.intellij.plugin.editor.report.actions.LayoutSwitchAction
import org.jqassistant.tooling.intellij.plugin.editor.report.actions.RefreshAction
import org.jqassistant.tooling.intellij.plugin.editor.report.actions.SearchAction
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.AbstractTableModel

class ReportToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
    private val report: JqassistantReport,
) {
    val contentPanel: JPanel
    val splitter: JBSplitter

    init {
        val projectTrees = buildTreePanels()

        val firstTree = projectTrees.first()
        val scrollableTree =
            if (projectTrees.size == 1) {
                JBScrollPane(firstTree)
            } else {
                val subPanel = JPanel()
                for (tree in projectTrees) subPanel.add(tree)

                JBScrollPane(subPanel)
            }

        val toolWindowPanel = SimpleToolWindowPanel(true)

        splitter = OnePixelSplitter(true)
        splitter.firstComponent = scrollableTree

        val actionManager = ActionManager.getInstance()

        val actionGroup =
            DefaultActionGroup(LayoutSwitchAction(this), RefreshAction(project, toolWindow), SearchAction())

        val actionToolbar =
            actionManager.createActionToolbar("jQAssistantReport Toolbar", actionGroup, true)
        actionToolbar.targetComponent = toolWindowPanel
        toolWindowPanel.toolbar = actionToolbar.component

        toolWindowPanel.setContent(splitter)

        contentPanel = toolWindowPanel
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

    private fun buildRuleTree(currentRoot: ReportNode?, currentReport: List<ReferencableRuleType>): List<ReportNode> {
        val nodeList = mutableListOf<ReportNode>()

        val groups = currentReport.filterIsInstance<GroupType>()
        val constraints = currentReport.filterIsInstance<ConstraintType>()
        val concepts = currentReport.filterIsInstance<ConceptType>()

        for (group in groups) {
            val newNode = ReferencableRuleTypeNode(group, currentRoot)
            nodeList.add(newNode)
            buildRuleTree(newNode, group.groupOrConceptOrConstraint)
            currentRoot?.addChild(newNode)
        }

        if (constraints.isNotEmpty()) {
            val groupingNode = GroupingNode("Constraints", currentRoot)
            nodeList.add(groupingNode)

            for (constraint in constraints) {
                val newNode = ReferencableRuleTypeNode(constraint, groupingNode)
                nodeList.add(newNode)
                groupingNode.addChild(newNode)
            }

            currentRoot?.addChild(groupingNode)
        }

        if (concepts.isNotEmpty()) {
            val groupingNode = GroupingNode("Concepts", currentRoot)
            nodeList.add(groupingNode)

            for (concept in concepts) {
                val newNode = ReferencableRuleTypeNode(concept, groupingNode)
                nodeList.add(newNode)
                groupingNode.addChild(newNode)
            }

            currentRoot?.addChild(groupingNode)
        }

        return nodeList
    }

    private fun treeClickListener(event: TreeSelectionEvent) {
        val reportNode = event.path.lastPathComponent as? ReferencableRuleTypeNode ?: return

        if (reportNode.ref is GroupType) {
            if (DumbService.isDumb(project)) return

            val ruleId = reportNode.ref.id
            val ruleIndexingService = project.service<JqaRuleIndexingService>()

            getApplication().executeOnPooledThread {
                val navigationElement =
                    ReadAction.compute<Navigatable?, Throwable> {
                        val definition = ruleIndexingService.resolve(ruleId) ?: return@compute null

                        val source = definition.computeSource() ?: return@compute null

                        source.navigationElement as? Navigatable
                    }

                getApplication().invokeLater {
                    if (navigationElement == null || !navigationElement.canNavigate()) return@invokeLater

                    navigationElement.navigate(true)
                }
            }
        }

        val result =
            when (val rule = reportNode.ref) {
                is ConstraintType -> rule.result
                is ConceptType -> rule.result
                else -> null
            }

        if (result == null) {
            splitter.secondComponent = null
            return
        }

        val columnNames = result.columns.column
        val rowData =
            result.rows.row.map { row ->
                row.column
                    .map { col ->
                        col.value
                    }.toTypedArray()
            }

        val table =
            JBTable(
                object : AbstractTableModel() {
                    override fun getColumnName(column: Int): String = columnNames[column].toString()

                    override fun getRowCount(): Int = rowData.size

                    override fun getColumnCount(): Int = columnNames.size

                    override fun getValueAt(row: Int, col: Int): Any = rowData[row][col]

                    override fun isCellEditable(row: Int, column: Int): Boolean = false

                    override fun setValueAt(value: Any, row: Int, col: Int) {
                        rowData[row][col] = value.toString()
                        this.fireTableCellUpdated(row, col)
                    }
                },
            )

        val tableSpeedSearch = TableSpeedSearch(table)

        splitter.secondComponent = JBScrollPane(tableSpeedSearch.component)
    }
}
