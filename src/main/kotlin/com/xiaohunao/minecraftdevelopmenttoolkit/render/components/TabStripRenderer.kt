package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.LocalDefaultTabStyle

val TabStripRenderer: @Composable (RendererContext) -> Unit = { context ->
    var tabs by remember { mutableStateOf(listOf<String>()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabsBinding = context.node.bindings?.find { it.prop == "tabs" }
    val selectedBinding = context.node.bindings?.find { it.prop == "selectedTab" }

    if (tabsBinding != null) {
        DisposableEffect(tabsBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                tabs = when {
                    el == null -> emptyList()
                    el.isJsonArray -> el.asJsonArray.map { if (it.isJsonPrimitive) it.asString else it.toString() }
                    else -> listOf(el.asString)
                }
            }
            context.stateStore.subscribe(tabsBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(tabsBinding.path, listener) }
        }
    }

    if (selectedBinding != null) {
        DisposableEffect(selectedBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                selectedTab = el?.let { if (it.isJsonPrimitive) it.asInt else 0 } ?: 0
            }
            context.stateStore.subscribe(selectedBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(selectedBinding.path, listener) }
        }
    }

    if (tabs.isNotEmpty()) {
        TabStrip(
            tabs = tabs.mapIndexed { index, tab ->
                TabData.Default(
                    selected = index == selectedTab,
                    content = { Text(tab) },
                    closable = false,
                    onClose = {},
                    onClick = {
                        selectedTab = index
                        if (context.node.events?.contains("onTabChange") == true) {
                            context.onEvent(context.node.id, "onTabChange", com.google.gson.JsonPrimitive(index))
                        }
                    }
                )
            },
            style = LocalDefaultTabStyle.current,
            modifier = Modifier.fillMaxWidth().padding(2.dp)
        )
    }
}
