package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.Text

val LazyColumnRenderer: @Composable (RendererContext) -> Unit = { context ->
    val itemsBinding = context.node.bindings?.find { it.prop == "items" }
    val selectedBinding = context.node.bindings?.find { it.prop == "selectedItem" }

    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }

    // Subscribe to items state
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

    // Subscribe to selectedItem state
    if (selectedBinding != null) {
        DisposableEffect(selectedBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                selected = el?.let { if (it.isJsonPrimitive) it.asString else null }
            }
            context.stateStore.subscribe(selectedBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(selectedBinding.path, listener) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(2.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selected

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .clickable {
                        selected = item
                        if (context.node.events?.contains("onSelect") == true) {
                            val jsonValue = com.google.gson.JsonPrimitive(item)
                            context.onEvent(context.node.id, "onSelect", jsonValue)
                        }
                    }
                    .then(
                        if (isSelected) Modifier.background(
                            androidx.compose.ui.graphics.Color(0x1A3B82F6)
                        ) else Modifier
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = item,
                    fontSize = 12.sp
                )
            }
        }
    }
}
