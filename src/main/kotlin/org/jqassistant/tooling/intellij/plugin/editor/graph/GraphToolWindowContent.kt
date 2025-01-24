package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.buschmais.jqassistant.core.rule.api.model.Concept
import com.buschmais.jqassistant.core.rule.api.model.Constraint
import com.buschmais.jqassistant.core.rule.api.model.Group
import com.buschmais.jqassistant.core.rule.api.model.Rule
import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.Graphs
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.ui.layout.Layouts
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.view.Viewer
import org.jqassistant.tooling.intellij.plugin.common.findRuleById
import org.jqassistant.tooling.intellij.plugin.common.getAllRules

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true) {
    private val displayGraph = SingleGraph("displayGraph", false, false)
    private val ruleGraph = SingleGraph("ruleGraph", false, false)

    init {
        System.setProperty("org.graphstream.ui", "swing")
        System.setProperty("sun.java2d.opengl", "True")

        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        // Initial graph that displays help text
        val background = UIUtil.getPanelBackground()
        val textColor = colorsScheme.defaultForeground

        displayGraph.setAttribute("ui.antialias")
        displayGraph.setAttribute(
            "ui.stylesheet",
            """
            graph {
                fill-color: rgb(${background.red}, ${background.green}, ${background.blue});
            }
            node {
                fill-mode: none;
                text-size: 20;
                text-color: rgb(${textColor.red}, ${textColor.green}, ${textColor.blue});
            }
            """.trimIndent(),
        )

        val viewer = SwingViewer(displayGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        displayGraph.addNode("help-text").setAttribute("ui.label", "Use the the \"Open Rule Graph\" right click action")

        val layout = Layouts.newLayoutAlgorithm()
        viewer.enableAutoLayout(layout)

        // false indicates "no JFrame".
        val view = viewer.addDefaultView(false) as DefaultView

        this.setContent(view)
    }

    companion object {
        const val FONT_SIZE: Int = 10

        // Should be 2/3 of FONT_SIZE rounded up
        const val CHARACTER_WIDTH: Int = ((FONT_SIZE * 2) / 3) + 1
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

        val textOffset = "0px, -${FONT_SIZE / 2}px"

        return """
            graph {
                fill-color: $rgbBackground;
                padding: 200px;
            }

            node {
                /* By using a mono font we can calculate the width */
                text-font: "Noto Sans Mono";
                text-size: $FONT_SIZE;
                text-color: $rgbText;
            }

            node.concept {
                shape: box;
                text-offset: $textOffset;

                fill-color: $rgbConcept;
            }

            node.constraint {
                shape: circle;
                text-offset: $textOffset;

                fill-color: $rgbConstraint;
            }

            node.group {
                shape: diamond;
                text-offset: $textOffset;

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
        val node = ruleGraph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.class", "group")

        // Character width of 9px for 15px font -> 2/3 of font size
        // See: https://stackoverflow.com/a/56379770/18448953
        val width: Int = ((name.length + 9) * CHARACTER_WIDTH)
        val height: Int = FONT_SIZE * 6
        node.setAttribute("ui.style", "size: ${width}px, ${height}px;")

        return node
    }

    private fun conceptNode(name: String): Node {
        val node = ruleGraph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.class", "concept")

        val width: Int = ((name.length) * CHARACTER_WIDTH)
        val height: Int = FONT_SIZE * 2
        node.setAttribute("ui.style", "size: ${width}px, ${height}px;")

        return node
    }

    private fun constraintNode(name: String): Node {
        val node = ruleGraph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.class", "constraint")

        val width: Int = ((name.length + 2) * CHARACTER_WIDTH)
        val height: Int = FONT_SIZE * 4
        node.setAttribute("ui.style", "size: ${width}px, ${height}px;")

        return node
    }

    private fun buildGraph(currentRuleId: String, ruleSet: RuleSet): Node? {
        val rule = ruleSet.findRuleById(currentRuleId) ?: return null
        return buildGraph(rule, ruleSet)
    }

    /**
     * Recursively builds up the dependency graph of a [RuleSet] starting at the
     * provided [Rule]. The return value is the created center [Node] if one
     * was created or already existing.
     */
    private fun buildGraph(currentRule: Rule, ruleSet: RuleSet): Node? {
        val currentRuleId = currentRule.id

        // Check if this node was already visited
        val existingNode = ruleGraph.getNode(currentRuleId)
        if (existingNode != null) return existingNode

        val currentNode =
            when (currentRule) {
                is Concept -> {
                    val currentNode = conceptNode(currentRuleId)

                    // TODO: Highlight that some relationships are optional
                    for (name in currentRule.requiresConcepts.keys) {
                        val newNode = buildGraph(name, ruleSet) ?: continue

                        val e = ruleGraph.addEdge("$currentRuleId->$name", currentNode, newNode, true)

                        e.setAttribute("ui.label", "requires")
                    }

                    for (name in currentRule.providedConcepts) {
                        val newNode = buildGraph(name, ruleSet) ?: continue
                        newNode.setAttribute("ui.class", "providesConcept", "concept")

                        val e = ruleGraph.addEdge("$currentRuleId<-$name", currentNode, newNode, true)
                        e.setAttribute("ui.label", "provides")
                    }

                    currentNode
                }

                is Constraint -> {
                    val currentNode = constraintNode(currentRuleId)
                    currentNode.setAttribute("ui.class", "constraint", "center")

                    // TODO: Highlight that some relationships are optional
                    for (name in currentRule.requiresConcepts.keys) {
                        val newNode = buildGraph(name, ruleSet) ?: continue

                        val e = ruleGraph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                        e.setAttribute("ui.label", "requires")
                    }

                    currentNode
                }

                is Group -> {
                    val currentNode = groupNode(currentRuleId)
                    currentNode.setAttribute("ui.class", "group", "center")

                    for (name in currentRule.groups.keys) {
                        val newNode = buildGraph(name, ruleSet) ?: continue

                        val e = ruleGraph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                        e.setAttribute("ui.label", "include")
                        // e.setAttribute("layout.weight", 5)
                    }

                    for (name in currentRule.concepts.keys) {
                        val newNode = buildGraph(name, ruleSet) ?: continue

                        val e = ruleGraph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                        e.setAttribute("ui.label", "include")
                    }

                    for (name in currentRule.constraints.keys) {
                        val newNode = buildGraph(name, ruleSet) ?: continue

                        val e = ruleGraph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                        e.setAttribute("ui.label", "include")
                    }

                    currentNode
                }

                else -> return null
            }

        return currentNode
    }

    /**
     * Replace the currently displayed graph with a new one that displays
     * the given [Rule]
     */
    fun refreshGraph(centerRule: Rule, ruleSet: RuleSet) {
        ruleGraph.clear()

        // Build rule graph
        for (rule in ruleSet.getAllRules()) {
            buildGraph(rule, ruleSet)
        }

        // Set weights for layout
        for (node in ruleGraph.nodes()) {
            node.setAttribute("layout.weight", 10)
        }

        for (edge in ruleGraph.edges()) {
            edge.setAttribute("layout.weight", 2)
        }

        // Build display graph
        displayGraph.clear()
        // Avoids a reassignment to displayGraph
        Graphs.mergeIn(displayGraph, ruleGraph)

        val centerNode = displayGraph.getNode(centerRule.id)

        val directNeighbours =
            centerNode.enteringEdges().map { e -> e.sourceNode }.toList() +
                centerNode
                    .leavingEdges()
                    .map { e -> e.targetNode }
                    .toList()

        val secondDegreeNeighbours =
            directNeighbours.flatMap { node ->
                node.enteringEdges().map { e -> e.sourceNode }.toList() +
                    centerNode
                        .leavingEdges()
                        .map { e -> e.targetNode }
                        .toList()
            }

        val directNeighbourEdges = centerNode.edges().toList()
        val secondDegreeNeighbourEdges =
            directNeighbours.flatMap { node -> node.edges().toList() }
        val displayedEdges = directNeighbourEdges + secondDegreeNeighbourEdges
        val displayedEdgeIds = displayedEdges.map { edge -> edge.id }

        val displayedNodes = mutableSetOf<Node>(centerNode)
        displayedNodes.addAll(directNeighbours)
        displayedNodes.addAll(secondDegreeNeighbours)
        val displayedNodeIds = displayedNodes.map { node -> node.id }

        for (node in displayedNodes) {
            println(node.id)
        }

        // displayedNodes.addAll(centerNode.neighborNodes().flatMap { node -> node.neighborNodes() }.toList())
        for (edge in ruleGraph.edges()) {
            if (edge == null) continue
            if (edge.id in displayedEdgeIds) continue
            displayGraph.removeEdge(edge.id)
        }

        for (node in ruleGraph.nodes()) {
            if (node == null) continue
            if (node.id in displayedNodeIds) continue
            displayGraph.removeNode(node.id)
        }

        displayGraph.setAttribute("ui.antialias")
        displayGraph.setAttribute("ui.stylesheet", stylesheet())
    }
}
