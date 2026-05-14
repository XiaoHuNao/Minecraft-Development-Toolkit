package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text

val DropdownRenderer: @Composable (RendererContext) -> Unit = { context ->
    var items by remember { mutableStateOf(listOf<String>()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val enabled = context.node.props.getBoolean("enabled", true)

    val itemsBinding = context.node.bindings?.find { it.prop == "items" }
    val selectedBinding = context.node.bindings?.find { it.prop == "selectedItem" }

    if (itemsBinding != null) {
        DisposableEffect(itemsBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                items = when {
                    el == null -> emptyList()
                    el.isJsonArray -> el.asJsonArray.map { if (it.isJsonPrimitive) it.asString else it.toString() }
                    else -> listOf(el.asString)
                }
            }
            context.stateStore.subscribe(itemsBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(itemsBinding.path, listener) }
        }
    }

    if (selectedBinding != null) {
        DisposableEffect(selectedBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                val selectedString = el?.let { if (it.isJsonPrimitive) it.asString else "" } ?: ""
                selectedIndex = items.indexOf(selectedString)
            }
            context.stateStore.subscribe(selectedBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(selectedBinding.path, listener) }
        }
    }

    ListComboBox(
        items = items,
        selectedIndex = selectedIndex.coerceIn(-1, items.lastIndex),
        onSelectedItemChange = { newIndex ->
            selectedIndex = newIndex
            val newSelected = items.getOrNull(newIndex) ?: ""
            if (context.node.events?.contains("onSelectionChange") == true) {
                val jsonValue = com.google.gson.JsonPrimitive(newSelected)
                context.onEvent(context.node.id, "onSelectionChange", jsonValue)
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(2.dp)
    )
}
