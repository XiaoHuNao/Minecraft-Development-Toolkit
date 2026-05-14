package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

val TableRenderer: @Composable (RendererContext) -> Unit = { context ->
    var columns by remember { mutableStateOf(listOf<String>()) }
    var rows by remember { mutableStateOf(listOf<List<String>>()) }

    val columnsBinding = context.node.bindings?.find { it.prop == "columns" }
    val rowsBinding = context.node.bindings?.find { it.prop == "rows" }

    if (columnsBinding != null) {
        DisposableEffect(columnsBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                columns = when {
                    el == null -> emptyList()
                    el.isJsonArray -> el.asJsonArray.map { if (it.isJsonPrimitive) it.asString else it.toString() }
                    else -> listOf(el.asString)
                }
            }
            context.stateStore.subscribe(columnsBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(columnsBinding.path, listener) }
        }
    }

    if (rowsBinding != null) {
        DisposableEffect(rowsBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                rows = when {
                    el == null -> emptyList()
                    el.isJsonArray -> el.asJsonArray.map { rowEl ->
                        if (rowEl.isJsonArray) {
                            rowEl.asJsonArray.map { if (it.isJsonPrimitive) it.asString else it.toString() }
                        } else listOf(rowEl.asString)
                    }
                    else -> emptyList()
                }
            }
            context.stateStore.subscribe(rowsBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(rowsBinding.path, listener) }
        }
    }

    if (columns.isNotEmpty()) {
        val model = remember(columns, rows) {
            DefaultTableModel(
                rows.map { it.toTypedArray() }.toTypedArray(),
                columns.toTypedArray()
            )
        }

        SwingPanel(
            modifier = Modifier.fillMaxWidth().padding(2.dp),
            factory = {
                JTable(model).apply {
                    autoCreateRowSorter = true
                    rowHeight = 24
                }
            }
        )
    }
}
