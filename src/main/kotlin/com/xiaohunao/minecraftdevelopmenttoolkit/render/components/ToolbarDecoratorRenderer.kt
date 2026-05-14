package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel

val ToolbarDecoratorRenderer: @Composable (RendererContext) -> Unit = { context ->
    var items by remember { mutableStateOf(listOf<String>()) }
    var addEnabled by remember { mutableStateOf(true) }
    var removeEnabled by remember { mutableStateOf(true) }

    // Bindings
    val itemsBinding = context.node.bindings?.find { it.prop == "items" }
    val addEnabledBinding = context.node.bindings?.find { it.prop == "addEnabled" }
    val removeEnabledBinding = context.node.bindings?.find { it.prop == "removeEnabled" }

    // Subscribe to items state changes
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

    // Subscribe to addEnabled state changes
    if (addEnabledBinding != null) {
        DisposableEffect(addEnabledBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                addEnabled = el?.let {
                    if (it.isJsonPrimitive) it.asBoolean else true
                } ?: true
            }
            context.stateStore.subscribe(addEnabledBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(addEnabledBinding.path, listener) }
        }
    }

    // Subscribe to removeEnabled state changes
    if (removeEnabledBinding != null) {
        DisposableEffect(removeEnabledBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                removeEnabled = el?.let {
                    if (it.isJsonPrimitive) it.asBoolean else true
                } ?: true
            }
            context.stateStore.subscribe(removeEnabledBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(removeEnabledBinding.path, listener) }
        }
    }

    // Build list model from items
    val model = remember(items) {
        DefaultListModel<String>().apply {
            items.forEach { addElement(it) }
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxWidth().padding(2.dp),
        factory = {
            val list = JBList(model).apply {
                selectionMode = ListSelectionModel.SINGLE_SELECTION
            }
            ToolbarDecorator.createDecorator(list)
                .setAddAction {
                    if (context.node.events?.contains("onAdd") == true) {
                        context.onEvent(context.node.id, "onAdd", null)
                    }
                }
                .setRemoveAction {
                    if (context.node.events?.contains("onRemove") == true) {
                        val selected = list.selectedValue
                        context.onEvent(
                            context.node.id, "onRemove",
                            if (selected != null) JsonPrimitive(selected) else null
                        )
                    }
                }
                .createPanel()
        }
    )
}
