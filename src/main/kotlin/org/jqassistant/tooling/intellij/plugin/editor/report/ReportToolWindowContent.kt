package org.jqassistant.tooling.intellij.plugin.editor.report

import com.intellij.find.SearchTextArea
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
import org.jqassistant.tooling.intellij.plugin.editor.report.actions.SortingToggleAction
import java.awt.BorderLayout
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.GroupingNode
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReferencableRuleTypeNode
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportCellRenderer
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportNode
import org.jqassistant.tooling.intellij.plugin.editor.report.tree.ReportTreeModel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.table.AbstractTableModel

class ReportToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
    private val report: JqassistantReport,
) {
    val toolWindowPanel: JPanel
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

        toolWindowPanel = SimpleToolWindowPanel(true)

        splitter = OnePixelSplitter(true)
        splitter.firstComponent = scrollableTree

        val actionManager = ActionManager.getInstance()

        val actionGroup =
            DefaultActionGroup(
                LayoutSwitchAction(this),
                RefreshAction(project, toolWindow),
                SortingToggleAction(projectTrees),
                SearchAction(projectTrees),
            )

        val actionToolbar =
            actionManager.createActionToolbar("jQAssistantReport Toolbar", actionGroup, true)
        actionToolbar.targetComponent = toolWindowPanel

        // Add search bar and handle text changes
        val textArea = JTextArea()
        val searchBar = SearchTextArea(textArea, false)
        textArea.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(p0: DocumentEvent?) {
                    updateSearch(p0)
                }

                override fun removeUpdate(p0: DocumentEvent?) {
                    updateSearch(p0)
                }

                override fun changedUpdate(p0: DocumentEvent?) {
                    updateSearch(p0)
                }

                fun updateSearch(event: DocumentEvent?) {
                    val newText = textArea.text

                    for (tree in projectTrees) {
                        val model = tree.model as? ReportTreeModel ?: continue
                        model.searchText = newText
                    }
                }
            },
        )

        val toolBarSplitter = OnePixelSplitter(false)
        toolBarSplitter.firstComponent = actionToolbar.component
        toolBarSplitter.secondComponent = searchBar

        toolWindowPanel.toolbar = toolBarSplitter

        // Banner for outdated report
        val outdatedReportBanner = OutdatedReportBanner(project, toolWindow)

        toolWindowPanel.add(outdatedReportBanner, BorderLayout.SOUTH)
        toolWindowPanel.setContent(splitter)
    }

    /**
     * Builds a tree panel for every root object of the report
     */
    private fun buildTreePanels(): List<Tree> {
        val nodeList = buildRuleTree(null, report.groupOrConceptOrConstraint)
        val cellRenderer = ReportCellRenderer()

        return nodeList.map { rootNode ->
            val treeModel = ReportTreeModel(rootNode)
            val treePanel = Tree(treeModel)

            TreeUIHelper.getInstance().installTreeSpeedSearch(treePanel)
            TreeUIHelper.getInstance().installSelectionSaver(treePanel)
            TreeUIHelper.getInstance().installSmartExpander(treePanel)
            treePanel.cellRenderer = cellRenderer

            treePanel.addTreeSelectionListener(::treeSelectionListener)

            // Listen to double-clicks
            // addTreeSelectionListener(::treeClickListener) does not allow
            // differentiating between normal and double clicks
            treePanel.addMouseListener(
                object : MouseListener {
                    override fun mouseClicked(event: java.awt.event.MouseEvent?) {
                        if (event == null) return

                        treeClickListener(treePanel, event)
                    }

                    override fun mousePressed(p0: java.awt.event.MouseEvent?) {}

                    override fun mouseReleased(p0: java.awt.event.MouseEvent?) {}

                    override fun mouseEntered(p0: java.awt.event.MouseEvent?) {}

                    override fun mouseExited(p0: java.awt.event.MouseEvent?) {}
                },
            )

            treePanel
        }
    }

    /**
     * This recursively builds a tree of ReportNodes using the currentRoot parameter as the root to append nodes to.
     * @param currentReport The ReportNode to which all new ReportNodes should be appended to
     * @param currentReport A list of rule reports that come directly after the currentRoot
     */
    private fun buildRuleTree(currentRoot: ReportNode?, currentReport: List<ReferencableRuleType>): List<ReportNode> {
        val nodeList = mutableListOf<ReportNode>()

        val groups = currentReport.filterIsInstance<GroupType>()
        val constraints = currentReport.filterIsInstance<ConstraintType>()
        val concepts = currentReport.filterIsInstance<ConceptType>()

        for (group in groups) {
            val newNode = ReferencableRuleTypeNode(group, currentRoot)
            nodeList.add(newNode)
            buildRuleTree(newNode, group.groupOrConceptOrConstraint)
            currentRoot?.add(newNode)
        }

        if (constraints.isNotEmpty()) {
            val groupingNode = GroupingNode("Constraints", currentRoot)
            nodeList.add(groupingNode)

            for (constraint in constraints) {
                val newNode = ReferencableRuleTypeNode(constraint, groupingNode)
                nodeList.add(newNode)
                groupingNode.add(newNode)
            }

            currentRoot?.add(groupingNode)
        }

        if (concepts.isNotEmpty()) {
            val groupingNode = GroupingNode("Concepts", currentRoot)
            nodeList.add(groupingNode)

            for (concept in concepts) {
                val newNode = ReferencableRuleTypeNode(concept, groupingNode)
                nodeList.add(newNode)
                groupingNode.add(newNode)
            }

            currentRoot?.add(groupingNode)
        }

        return nodeList
    }

    private fun treeSelectionListener(event: TreeSelectionEvent) {
        val node = event.path.lastPathComponent ?: return
        val reportNode = node as? ReferencableRuleTypeNode ?: return

        // Expand constraint/concept results on single click
        val result =
            when (val rule = reportNode.ref) {
                is ConstraintType -> rule.result
                is ConceptType -> rule.result
                else -> null
            }

        // Clear report results table when no results are available
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

    private fun treeClickListener(tree: Tree, event: MouseEvent) {
        val node = tree.selectionPath?.lastPathComponent ?: return
        val reportNode = node as? ReferencableRuleTypeNode ?: return

        // Navigate to rule on double click
        if (event.clickCount == 2) {
            if (DumbService.isDumb(project)) return

            val ruleId = reportNode.ref.id
            val ruleIndexingService = project.service<JqaRuleIndexingService>()

            getApplication().executeOnPooledThread {
                val navigationElement =
                    ReadAction.compute<Navigatable?, Throwable> {
                        val definition = ruleIndexingService.resolve(ruleId).firstOrNull() ?: return@compute null

                        val source = definition.computeSource() ?: return@compute null

                        source.navigationElement as? Navigatable
                    }

                getApplication().invokeLater {
                    if (navigationElement == null || !navigationElement.canNavigate()) return@invokeLater

                    navigationElement.navigate(true)
                }
            }
        }
    }
}
