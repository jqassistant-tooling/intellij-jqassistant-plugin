package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.util.ui.UIUtil
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.layout.Layouts
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.view.Viewer
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Concept
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Constraint
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Group
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true) {
    var currentRule: RuleBase? = null

    private val graph = MultiGraph("toolWindowGraph")

    init {

        val G = graph.addNode("Graph")
        G.setAttribute("ui.label", "Graph")
        G.setAttribute("xy", 1, 3)

        val V = graph.addNode("Viewer")
        V.setAttribute("ui.label", "V")
        V.setAttribute("xy", 3, 3)

        val P1 = graph.addNode("GtoV")
        P1.setAttribute("ui.label", "P1")
        val P2 = graph.addNode("VtoG")
        P2.setAttribute("ui.label", "P2")

        graph.addEdge("G->GtoV", "Graph", "GtoV", true)
        graph.addEdge("GtoV->V", "GtoV", "Viewer", true)
        graph.addEdge("VtoG<-V", "Viewer", "VtoG", true)
        graph.addEdge("G<-VtoG", "VtoG", "Graph", true)

        System.setProperty("org.graphstream.ui", "swing")
        System.setProperty("sun.java2d.opengl", "True")

        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        val background = UIUtil.getPanelBackground()
        val textColor = colorsScheme.defaultForeground

        graph.setAttribute("ui.antialias")
        graph.setAttribute(
            "ui.stylesheet",
            """
            graph {
                fill-color: rgb(${background.red}, ${background.green}, ${background.blue});
            }
            node {
            	size: 90px, 35px;
            	shape: box;
                fill-color: rgb(${background.red}, ${background.green}, ${background.blue});
            	stroke-mode: plain;
                stroke-color: rgb(${textColor.red}, ${textColor.green}, ${textColor.blue});
                text-color: rgb(${textColor.red}, ${textColor.green}, ${textColor.blue});
            }
            edge {
                fill-color: rgb(${textColor.red}, ${textColor.green}, ${textColor.blue});
            }
            """.trimIndent(),
        )

        val viewer = SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)

        val layout = Layouts.newLayoutAlgorithm()
        viewer.enableAutoLayout(layout)

        // false indicates "no JFrame".
        val view = viewer.addDefaultView(false) as DefaultView

        this.setContent(view)
    }

    fun buildGraph() {
        graph.clear()

        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        val backgroundColor = UIUtil.getPanelBackground()
        val textColor = colorsScheme.defaultForeground

        graph.setAttribute("ui.antialias")
        val rgbBackground = "rgb(${backgroundColor.red}, ${backgroundColor.green}, ${backgroundColor.blue})"
        val rgbText = "rgb(${textColor.red}, ${textColor.green}, ${textColor.blue})"

        graph.setAttribute(
            "ui.stylesheet",
            """
            graph {
                fill-color: $rgbBackground;
            }
            node {
                text-color: $rgbText;
                text-padding: 30px;
                text-background-mode: plain;
                text-background-color: $rgbBackground;
            }
            edge {
                fill-color: $rgbText;
                text-color: $rgbText;
                text-padding: 30px;
                text-background-mode: plain;
                text-alignment: along;
                text-background-color: $rgbBackground;
            }
            """.trimIndent(),
        )

        if (currentRule == null) return

        val currentRuleId = currentRule!!.id.stringValue
        val centerNode = graph.addNode(currentRuleId)
        centerNode.setAttribute("ui.label", currentRuleId)

        when (currentRule) {
            is Concept -> {
                // Multithreading
                val currentRule = currentRule as? Concept ?: return

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }

                for (concept in currentRule.providesConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId<-$name", name, currentRuleId, true)
                    e.setAttribute("ui.label", "providesConcept")
                }
            }

            is Constraint -> {
                // Multithreading
                val currentRule = currentRule as? Constraint ?: return

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }
            }

            is Group -> {
                val currentRule = currentRule as? Group ?: return

                for (group in currentRule.includeGroup) {
                    val name = group.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeGroup")
                }

                for (concept in currentRule.includeConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId->$name", name, currentRuleId, true)
                    e.setAttribute("ui.label", "includeConcept")
                }

                for (constraint in currentRule.includeConstraint) {
                    val name = constraint.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeConstraint")
                }
            }
        }
    }
}
