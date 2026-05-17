package com.xiaohunao.minecraftdevelopmenttoolkit.render

import androidx.compose.runtime.Composable
import com.google.gson.JsonElement
import com.xiaohunao.mdt.protocol.ComponentNode

data class RendererContext(
    val node: ComponentNode,
    val stateStore: StateStore,
    val onEvent: (componentId: String, eventType: String, value: JsonElement?) -> Unit,
    val componentMap: Map<String, ComponentNode> = emptyMap(),
    val renderEngine: RenderEngine? = null,
    val tabId: String = ""
) {
    @Composable
    fun RenderChildren() {
        val engine = renderEngine ?: return
        node.children.forEach { childId ->
            val childNode = componentMap[childId]
            if (childNode != null) {
                engine.renderNode(childNode, componentMap, stateStore, tabId)
            }
        }
    }
}

class ComponentRegistry {
    private val renderers = mutableMapOf<String, @Composable (RendererContext) -> Unit>()

    fun register(type: String, renderer: @Composable (RendererContext) -> Unit) {
        renderers[type] = renderer
    }

    fun getRenderer(type: String): (@Composable (RendererContext) -> Unit)? {
        return renderers[type]
    }

    fun hasRenderer(type: String): Boolean = renderers.containsKey(type)
}
