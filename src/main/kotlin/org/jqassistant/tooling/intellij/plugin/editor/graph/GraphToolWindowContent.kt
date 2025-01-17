package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.buschmais.jqassistant.core.rule.api.model.Rule
import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ColorUtil
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.UIUtil
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.layout.Layouts
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.view.Viewer
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Concept
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Constraint
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.Group

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true) {
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

        // splitter.firstComponent = textArea
        splitter.secondComponent = view

        this.setContent(splitter)
    }

    // Based on: https://graphstream-project.org/doc/Advanced-Concepts/GraphStream-CSS-Reference/
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

        val textSize: Int = 15
        val textOffset = "0px, -${textSize / 2}px"

        return """
            graph {
                fill-color: $rgbBackground;
                padding: 200px;
            }

            node {
                /* By using a mono font we can calculate the width */
                text-font: "Noto Sans Mono";
                text-size: $textSize;
                text-color: $rgbText;
            }

            node.concept {
                /* padding: 10px, 5px; */
                /* Only necessary for arrows */
                size-mode: fit;
                shape: rounded-box;
                fill-mode: none;

                text-padding: ${textSize / 2}px;
                text-background-mode: rounded-box;
                text-background-color: $rgbConcept;
            }

            node.constraint {
                shape: circle;
                /* size: 300px, 40px; */
                text-offset: $textOffset;

                fill-color: $rgbConstraint;
            }

            node.group {
                /* size: 200px, 60px; */
                text-offset: $textOffset;
                shape: diamond;

                fill-color: $rgbGroup;
            }

            edge {
                shape: blob;

                fill-color: $rgbText;
                text-color: $rgbText;

                text-size: 10;
                text-padding: 5px;
                text-offset: 0px, -5px;
                text-background-mode: plain;
                text-alignment: along;
                text-background-color: $rgbBackground;
            }
            """.trimIndent()
    }

    private fun groupNode(name: String): Node {
        val node = graph.addNode(name)
        node.setAttribute("ui.label", name)
        // Character width of 9px
        // See: https://stackoverflow.com/a/56379770/18448953
        node.setAttribute("ui.style", "size: ${(name.length * 10) + 90}px, 90px;")

        return node
    }

    private fun conceptNode(name: String): Node {
        val node = graph.addNode(name)
        node.setAttribute("ui.label", name)

        return node
    }

    private fun constraintNode(name: String): Node {
        val node = graph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.style", "size: ${(name.length * 10) + 15}px, 60px;")

        return node
    }

    /**
     * Recursively builds up the dependency graph of a [RuleSet] starting at the
     * provided [Rule]
     */
    fun buildGraph(centerRule: Rule, ruleSet: RuleSet) {
        val currentRuleId = centerRule.id

        when (centerRule) {
            is Concept -> {
                // Multithreading
                val currentRule = centerRule as? Concept ?: return
                val centerNode = conceptNode(currentRuleId)
                centerNode.setAttribute("ui.class", "concept", "center")

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value ?: "NULL"
                    val n = conceptNode(name)
                    n.setAttribute("ui.class", "requiresConcept", "concept")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }

                for (concept in currentRule.providesConcept) {
                    val name = concept.refType.value ?: "NULL"
                    val n = conceptNode(name)
                    n.setAttribute("ui.class", "providesConcept", "concept")

                    val e = graph.addEdge("$currentRuleId<-$name", name, currentRuleId, true)
                    e.setAttribute("ui.label", "providesConcept")
                }
            }

            is Constraint -> {
                // Multithreading
                val currentRule = centerRule as? Constraint ?: return
                val centerNode = constraintNode(currentRuleId)
                centerNode.setAttribute("ui.class", "constraint", "center")

                for (concept in currentRule.requiresConcept) {
                    val name = concept.refType.value ?: "NULL"
                    val n = conceptNode(name)
                    n.setAttribute("ui.class", "requiresConcept", "concept")
                    n.setAttribute("ui.style", "size: ${(name.length * 10) + 10}px, 50px;")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }
            }

            is Group -> {
                val currentRule = centerRule as? Group ?: return
                val centerNode = groupNode(currentRuleId)
                centerNode.setAttribute("ui.class", "group", "center")

                for (group in currentRule.includeGroup) {
                    val name = group.refType.value ?: "NULL"
                    val n = groupNode(name)
                    n.setAttribute("ui.class", "includeGroup", "group")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeGroup")
                }

                for (concept in currentRule.includeConcept) {
                    val name = concept.refType.value ?: "NULL"
                    val n = conceptNode(name)
                    n.setAttribute("ui.class", "includeConcept", "concept")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeConcept")
                }

                for (constraint in currentRule.includeConstraint) {
                    val name = constraint.refType.value ?: "NULL"
                    val n = constraintNode(name)
                    n.setAttribute("ui.class", "includeConstraint", "constraint")

                    val e = graph.addEdge("$currentRuleId->$name", currentRuleId, name, true)
                    e.setAttribute("ui.label", "includeConstraint")
                }
            }
        }
    }

    /**
     * Replace the currently displayed graph with a new one that displays
     * the given [Rule]
     */
    fun refreshGraph(centerRule: Rule, ruleSet: RuleSet) {
        graph.clear()

        graph.setAttribute("ui.antialias")
        graph.setAttribute("ui.stylesheet", stylesheet())

        buildGraph(centerRule, ruleSet)
    }
}
