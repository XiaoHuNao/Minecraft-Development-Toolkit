package com.xiaohunao.minecraftdevelopmenttoolkit.render

import androidx.compose.runtime.*
import com.google.gson.JsonElement
import com.xiaohunao.mdt.protocol.ComponentNode
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle

class RenderEngine(
    private val registry: ComponentRegistry,
    private val onEvent: (tabId: String, componentId: String, eventType: String, value: JsonElement?) -> Unit
) {

    fun renderTab(
        tabId: String,
        components: List<ComponentNode>,
        stateStore: StateStore
    ): @Composable () -> Unit {
        val componentMap = components.associateBy { it.id }
        val rootNodes = components.filter { node ->
            components.none { parent -> parent.children.contains(node.id) }
        }

        return {
            rootNodes.forEach { rootNode ->
                renderNode(rootNode, componentMap, stateStore, tabId)
            }
        }
    }

    @Composable
    fun renderNode(
        node: ComponentNode,
        componentMap: Map<String, ComponentNode>,
        stateStore: StateStore,
        tabId: String
    ) {
        val context = RendererContext(
            node = node,
            stateStore = stateStore,
            onEvent = { componentId, eventType, value ->
                onEvent(tabId, componentId, eventType, value)
            },
            componentMap = componentMap,
            renderEngine = this,
            tabId = tabId
        )

        val renderer = registry.getRenderer(node.type)
        if (renderer != null) {
            renderer(context)
        } else {
            UnknownComponentRenderer(context)
        }
    }
}

@Composable
fun UnknownComponentRenderer(context: RendererContext) {
    org.jetbrains.jewel.ui.component.Text(
        text = MyMessageBundle.message("render.unknown.component", context.node.type),
        color = androidx.compose.ui.graphics.Color.Gray,
        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
    com.intellij.openapi.diagnostic.Logger.getInstance(RenderEngine::class.java)
        .warn("Unknown component type: ${context.node.type}")
}
