package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text

val CheckboxRenderer: @Composable (RendererContext) -> Unit = { context ->
    val text = context.node.props.getString("text", "")
    var selected by remember { mutableStateOf(context.node.props.getBoolean("selected", false)) }
    val enabled = context.node.props.getBoolean("enabled", true)

    // Subscribe to state if bound
    val selectedBinding = context.node.bindings?.find { it.prop == "selected" }
    if (selectedBinding != null) {
        DisposableEffect(selectedBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                val newVal = el?.let { if (it.isJsonPrimitive) it.asBoolean else false } ?: false
                if (newVal != selected) selected = newVal
            }
            context.stateStore.subscribe(selectedBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(selectedBinding.path, listener) }
        }
    }

    Checkbox(
        checked = selected,
        onCheckedChange = { newChecked ->
            selected = newChecked
            if (context.node.events?.contains("onCheckedChange") == true) {
                val jsonValue = com.google.gson.JsonPrimitive(newChecked)
                context.onEvent(context.node.id, "onCheckedChange", jsonValue)
            }
        },
        enabled = enabled,
        modifier = Modifier.padding(2.dp)
    )
}
