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
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.layout.HierarchicalLayout
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.swing_viewer.SwingViewer
import org.graphstream.ui.view.Viewer
import org.jqassistant.tooling.intellij.plugin.common.findRuleById

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true) {
    private val graph = MultiGraph("toolWindowGraph")

    init {
        System.setProperty("org.graphstream.ui", "swing")
        System.setProperty("sun.java2d.opengl", "True")

        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        // Initial graph that displays help text
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
                fill-mode: none;
                text-size: 20;
                text-color: rgb(${textColor.red}, ${textColor.green}, ${textColor.blue});
            }
            """.trimIndent(),
        )

        val viewer = SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        graph.addNode("help-text").setAttribute("ui.label", "Use the the \"Open Rule Graph\" right click action")

        val layout = HierarchicalLayout()
        viewer.enableAutoLayout(layout)

        // false indicates "no JFrame".
        val view = viewer.addDefaultView(false) as DefaultView

        this.setContent(view)
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
        node.setAttribute("ui.class", "group")
        node.setAttribute("ui.style", "size: ${(name.length * 10) + 90}px, 90px;")

        return node
    }

    private fun conceptNode(name: String): Node {
        val node = graph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.class", "concept")

        return node
    }

    private fun constraintNode(name: String): Node {
        val node = graph.addNode(name)
        node.setAttribute("ui.label", name)
        node.setAttribute("ui.class", "constraint")
        node.setAttribute("ui.style", "size: ${(name.length * 10) + 15}px, 60px;")

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
        val existingNode = graph.getNode(currentRuleId)
        if (existingNode != null) return existingNode

        return when (currentRule) {
            is Concept -> {
                val currentNode = conceptNode(currentRuleId)

                // TODO: Highlight that some relationships are optional
                for (name in currentRule.requiresConcepts.keys) {
                    val newNode = buildGraph(name, ruleSet) ?: continue

                    val e = graph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }

                for (name in currentRule.providedConcepts) {
                    val newNode = buildGraph(name, ruleSet) ?: continue
                    newNode.setAttribute("ui.class", "providesConcept", "concept")

                    val e = graph.addEdge("$currentRuleId<-$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "providesConcept")
                }

                currentNode
            }

            is Constraint -> {
                val currentNode = constraintNode(currentRuleId)
                currentNode.setAttribute("ui.class", "constraint", "center")

                // TODO: Highlight that some relationships are optional
                for (name in currentRule.requiresConcepts.keys) {
                    val newNode = buildGraph(name, ruleSet) ?: continue

                    val e = graph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "requiresConcept")
                }

                currentNode
            }

            is Group -> {
                val currentNode = groupNode(currentRuleId)
                currentNode.setAttribute("ui.class", "group", "center")

                for (name in currentRule.groups.keys) {
                    val newNode = buildGraph(name, ruleSet) ?: continue

                    val e = graph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "includeGroup")
                }

                for (name in currentRule.concepts.keys) {
                    val newNode = buildGraph(name, ruleSet) ?: continue

                    val e = graph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "includeConcept")
                }

                for (name in currentRule.constraints.keys) {
                    val newNode = buildGraph(name, ruleSet) ?: continue

                    val e = graph.addEdge("$currentRuleId->$name", currentNode, newNode, true)
                    e.setAttribute("ui.label", "includeConstraint")
                }

                currentNode
            }

            else -> return null
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
