package org.jqassistant.tooling.intellij.plugin.editor.graph

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.view.mxGraph
import javax.swing.JPanel

class GraphToolWindowContent(
    private val project: Project,
    private val toolWindow: ToolWindow,
) {
    val toolWindowPanel: JPanel

    init {
        toolWindowPanel = SimpleToolWindowPanel(true)

        val graph = mxGraph()
        val parent = graph.defaultParent

        graph.model.beginUpdate()
        try {
            val v1 =
                graph.insertVertex(
                    parent,
                    null,
                    "Hello",
                    20.0,
                    20.0,
                    80.0,
                    30.0,
                )
            val v2 =
                graph.insertVertex(
                    parent,
                    null,
                    "World!",
                    240.0,
                    150.0,
                    80.0,
                    30.0,
                )
            graph.insertEdge(parent, null, "Edge", v1, v2)
        } finally {
            graph.model.endUpdate()
        }

        val graphComponent = mxGraphComponent(graph)

        toolWindowPanel.setContent(graphComponent)
    }
}
