package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.Slider

val SliderRenderer: @Composable (RendererContext) -> Unit = { context ->
    var value by remember { mutableFloatStateOf(context.node.props.getDouble("value", 0.0).toFloat()) }
    val min = context.node.props.getDouble("min", 0.0).toFloat()
    val max = context.node.props.getDouble("max", 100.0).toFloat()
    val steps = context.node.props.getInt("steps", 0)
    val enabled = context.node.props.getBoolean("enabled", true)

    val valueBinding = context.node.bindings?.find { it.prop == "value" }
    if (valueBinding != null) {
        DisposableEffect(valueBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                val newVal = el?.let { if (it.isJsonPrimitive) it.asFloat else 0f } ?: 0f
                if (newVal != value) value = newVal
            }
            context.stateStore.subscribe(valueBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(valueBinding.path, listener) }
        }
    }

    Slider(
        value = value,
        onValueChange = { newValue ->
            value = newValue
            if (context.node.events?.contains("onValueChange") == true) {
                val jsonValue = com.google.gson.JsonPrimitive(newValue.toDouble())
                context.onEvent(context.node.id, "onValueChange", jsonValue)
            }
        },
        valueRange = min..max,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(2.dp)
    )
}
