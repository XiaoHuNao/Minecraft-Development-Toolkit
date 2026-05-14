package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.CircularProgressIndicator

// TODO: Implement actual progress bar that respects `value` instead of always showing indeterminate spinner
val ProgressBarRenderer: @Composable (RendererContext) -> Unit = { context ->
    var value by remember { mutableStateOf(context.node.props.getDouble("value", 0.0)) }
    val indeterminate = context.node.props.getBoolean("indeterminate", false)

    val valueBinding = context.node.bindings?.find { it.prop == "value" }
    if (valueBinding != null) {
        DisposableEffect(valueBinding.path) {
            val listener: (com.google.gson.JsonElement?) -> Unit = { el ->
                val newVal = el?.let { if (it.isJsonPrimitive) it.asDouble else 0.0 } ?: 0.0
                if (newVal != value) value = newVal
            }
            context.stateStore.subscribe(valueBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(valueBinding.path, listener) }
        }
    }

    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(2.dp))
}
