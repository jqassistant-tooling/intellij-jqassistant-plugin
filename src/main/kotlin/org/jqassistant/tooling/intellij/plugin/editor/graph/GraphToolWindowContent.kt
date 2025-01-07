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
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.ReferenceType
import org.jqassistant.tooling.intellij.plugin.data.rules.xml.RuleBase

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) : SimpleToolWindowPanel(true) {
    var currentRule: RuleBase? = null
    var groups = mutableListOf<ReferenceType>()
    var constraints = mutableListOf<ReferenceType>()
    var concepts = mutableListOf<ReferenceType>()

    init {

        val graph = MultiGraph("embedded")

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

        graph.setAttribute("ui.antialias")

        val colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme

        val background = UIUtil.getPanelBackground()
        val textColor = colorsScheme.defaultForeground

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
}
