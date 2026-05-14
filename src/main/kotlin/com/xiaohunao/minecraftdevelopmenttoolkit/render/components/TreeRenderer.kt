package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import javax.swing.JTree
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

val TreeRenderer: @Composable (RendererContext) -> Unit = { context ->
    var rootNodeData by remember { mutableStateOf<JsonObject?>(null) }

    val rootBinding = context.node.bindings?.find { it.prop == "rootNode" }
    if (rootBinding != null) {
        DisposableEffect(rootBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                rootNodeData = if (el is JsonObject) el else null
            }
            context.stateStore.subscribe(rootBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(rootBinding.path, listener) }
        }
    }

    // Also try reading rootNode from props directly
    val propNode = context.node.props.getString("rootNode", "")
    if (rootNodeData == null && propNode.isNotEmpty()) {
        rootNodeData = try {
            com.google.gson.JsonParser.parseString(propNode).asJsonObject
        } catch (_: Exception) { null }
    }

    val treeModel = remember(rootNodeData) {
        val root = buildTreeNode(rootNodeData ?: JsonObject().apply { addProperty("label", "Root") })
        DefaultTreeModel(root)
    }

    SwingPanel(
        modifier = Modifier.fillMaxWidth().height(200.dp).padding(2.dp),
        factory = {
            JTree(treeModel).apply {
                addTreeSelectionListener(object : TreeSelectionListener {
                    override fun valueChanged(e: TreeSelectionEvent) {
                        val node = e.path.lastPathComponent as? DefaultMutableTreeNode
                        val label = node?.userObject as? String ?: return
                        if (context.node.events?.contains("onNodeSelect") == true) {
                            context.onEvent(
                                context.node.id,
                                "onNodeSelect",
                                com.google.gson.JsonPrimitive(label)
                            )
                        }
                    }
                })
            }
        }
    )
}

private fun buildTreeNode(json: JsonObject): DefaultMutableTreeNode {
    val label = json.get("label")?.let {
        if (it.isJsonPrimitive) it.asString else it.toString()
    } ?: "Node"
    val node = DefaultMutableTreeNode(label)
    val children = if (json.has("children") && json.get("children").isJsonArray)
        json.getAsJsonArray("children") else null
    children?.forEach { childEl ->
        if (childEl is JsonObject) {
            node.add(buildTreeNode(childEl))
        } else if (childEl.isJsonPrimitive) {
            node.add(DefaultMutableTreeNode(childEl.asString))
        }
    }
    return node
}
