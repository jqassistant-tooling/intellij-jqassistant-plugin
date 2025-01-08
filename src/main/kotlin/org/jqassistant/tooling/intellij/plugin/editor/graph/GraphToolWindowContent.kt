package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ColorUtil
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBTextArea
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

        val g = graph.addNode("Graph")
        g.setAttribute("ui.label", "Graph")
        g.setAttribute("xy", 1, 3)

        val v = graph.addNode("Viewer")
        v.setAttribute("ui.label", "V")
        v.setAttribute("xy", 3, 3)

        val p1 = graph.addNode("GtoV")
        p1.setAttribute("ui.label", "P1")
        val p2 = graph.addNode("VtoG")
        p2.setAttribute("ui.label", "P2")

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

        val splitter = OnePixelSplitter()
        val textArea = JBTextArea()
        textArea.text = stylesheet()

        textArea.document.whenTextChanged { event ->
            graph.removeAttribute("ui.stylesheet")
            graph.setAttribute("ui.stylesheet", textArea.text)
        }

        splitter.firstComponent = textArea
        splitter.secondComponent = view

        this.setContent(splitter)
    }

    private fun stylesheet(): String {
        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        val backgroundColor = UIUtil.getPanelBackground()
        val textColor = colorsScheme.defaultForeground

        val rgbBackground = "rgb(${backgroundColor.red}, ${backgroundColor.green}, ${backgroundColor.blue})"
        val rgbText = "rgb(${textColor.red}, ${textColor.green}, ${textColor.blue})"

        val blendingValue = 0.15
        val blendedColor =
            { col: String -> ColorUtil.blendColorsInRgb(backgroundColor, ColorUtil.fromHex(col), blendingValue) }

        val conceptColor = blendedColor("f4f02d")
        val constraintColor = blendedColor("f4612d")
        val groupColor = blendedColor("2df4d1")

        val rgbConcept = "rgb(${conceptColor.red}, ${conceptColor.green}, ${conceptColor.blue})"
        val rgbConstraint = "rgb(${constraintColor.red}, ${constraintColor.green}, ${constraintColor.blue})"
        val rgbGroup = "rgb(${groupColor.red}, ${groupColor.green}, ${groupColor.blue})"

        return """
            graph {
                fill-color: $rgbBackground;
                padding: 50px;
            }

            node {
                /* For blob */
                size: 20px;

                shape: rounded-box;
                fill-color: $rgbBackground;

                text-size: 15;
                text-color: $rgbText;
                text-padding: 10px, 5px;
                text-background-mode: rounded-box;
            }
            node.concept {
                text-background-color: $rgbConcept;
            }
            node.constraint {
                /* Custom shape for constraint */
                text-background-mode: none;
                size-mode: fit;
                shape: circle;
                fill-color: $rgbConstraint;
                text-offset: 0px, -30px;
                padding: 20px;

                /* text-background-color: $rgbConstraint; */
            }
            node.group {
                /* Custom shape for constraint */
                text-background-mode: none;
                size-mode: fit;
                shape: diamond;
                fill-color: $rgbGroup;
                text-offset: 0px, -80px;
                padding: 40px;

                text-background-color: $rgbGroup;
            }

            edge {
                shape: blob;

                fill-color: $rgbText;
                text-color: $rgbText;

                text-size: 10;
                text-padding: 5px;
                text-background-mode: plain;
                text-alignment: along;
                text-background-color: $rgbBackground;
            }
            """.trimIndent()
    }

    fun buildGraph() {
        graph.clear()

        graph.setAttribute("ui.antialias")
        graph.setAttribute("ui.stylesheet", stylesheet())

        if (currentRule == null) return

        val currentRuleId = currentRule!!.id.stringValue
        val centerNode = graph.addNode(currentRuleId)
        centerNode.setAttribute("ui.label", currentRuleId)

        when (currentRule) {
            is Concept -> {
                // Multithreading
                val currentRule = currentRule as? Concept ?: return
                centerNode.setAttribute("ui.class", "concept", "center")

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "requiresConcept", "concept")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }

                for (concept in currentRule.providesConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "providesConcept", "concept")

                    val e = graph.addEdge("$currentRuleId<-$name", name, currentRuleId, true)
                    e.setAttribute("ui.label", "providesConcept")
                }
            }

            is Constraint -> {
                // Multithreading
                val currentRule = currentRule as? Constraint ?: return
                centerNode.setAttribute("ui.class", "group", "center")

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "requiresConcept", "concept")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }
            }

            is Group -> {
                val currentRule = currentRule as? Group ?: return
                centerNode.setAttribute("ui.class", "group", "center")

                for (group in currentRule.includeGroup) {
                    val name = group.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "includeGroup", "group")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeGroup")
                }

                for (concept in currentRule.includeConcept) {
                    val name = concept.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "includeConcept", "concept")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeConcept")
                }

                for (constraint in currentRule.includeConstraint) {
                    val name = constraint.refType.value
                    val n = graph.addNode(name)
                    n.setAttribute("ui.label", name)
                    n.setAttribute("ui.class", "includeConstraint", "constraint")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeConstraint")
                }
            }
        }
    }
}
