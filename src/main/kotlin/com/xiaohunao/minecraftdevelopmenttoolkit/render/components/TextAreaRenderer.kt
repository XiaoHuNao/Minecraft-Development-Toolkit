package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import androidx.compose.foundation.text.input.TextFieldState

val TextAreaRenderer: @Composable (RendererContext) -> Unit = { context ->
    val placeholder = context.node.props.getString("placeholder", "")
    val enabled = context.node.props.getBoolean("enabled", true)
    val readOnly = context.node.props.getBoolean("readOnly", false)
    val rows = context.node.props.getInt("rows", 5)

    val valueBinding = context.node.bindings?.find { it.prop == "value" }
    val state = remember { TextFieldState() }
    var isSyncingFromServer by remember { mutableStateOf(false) }

    // Subscribe to server state updates
    if (valueBinding != null) {
        DisposableEffect(valueBinding.path) {
            val listener: (JsonElement?) -> Unit = { el ->
                val newValue = el?.let { if (it.isJsonPrimitive) it.asString else it.toString() } ?: ""
                if (newValue != state.text.toString()) {
                    isSyncingFromServer = true
                    state.edit {
                        replace(0, length, newValue)
                    }
                }
            }
            context.stateStore.subscribe(valueBinding.path, listener)
            onDispose { context.stateStore.unsubscribe(valueBinding.path, listener) }
        }
    }

    // Observe user input changes and fire events back to server
    val supportsOnValueChange = context.node.events?.contains("onValueChange") == true
    if (supportsOnValueChange) {
        LaunchedEffect(state) {
            snapshotFlow { state.text.toString() }
                .distinctUntilChanged()
                .collectLatest { newText ->
                    if (!isSyncingFromServer) {
                        context.onEvent(context.node.id, "onValueChange", JsonPrimitive(newText))
                    }
                    isSyncingFromServer = false
                }
        }
    }

    TextArea(
        state = state,
        placeholder = { Text(placeholder) },
        enabled = enabled,
        readOnly = readOnly,
        modifier = Modifier.fillMaxWidth().height((rows * 20).dp).padding(2.dp)
    )
}
