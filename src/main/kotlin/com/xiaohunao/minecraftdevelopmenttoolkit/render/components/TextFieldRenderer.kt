package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonPrimitive
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.selectAll

val TextFieldRenderer: @Composable (RendererContext) -> Unit = { context ->
    val placeholder = context.node.props.getString("placeholder", "")
    val enabled = context.node.props.getBoolean("enabled", true)
    val readOnly = context.node.props.getBoolean("readOnly", false)

    val valueBinding = context.node.bindings.find { it.prop == "value" }
    val state = remember { TextFieldState() }
    var isSyncingFromServer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Subscribe to server state updates
    if (valueBinding != null) {
        DisposableEffect(valueBinding.path) {
            val listener: (com.google.gson.JsonElement?) -> Unit = { el ->
                val newValue = el?.let { if (it.isJsonPrimitive) it.asString else it.toString() } ?: ""
                if (newValue != state.text.toString()) {
                    isSyncingFromServer = true
                    // Dispatch to Compose thread — listener fires from OkHttp callback
                    scope.launch {
                        state.edit {
                            replace(0, length, newValue)
                        }
                    }
                }
            }
            context.stateStore.subscribe(valueBinding.path, listener)
            onDispose {
                context.stateStore.unsubscribe(valueBinding.path, listener)
            }
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

    TextField(
        state = state,
        placeholder = { Text(placeholder) },
        enabled = enabled,
        readOnly = readOnly,
        modifier = Modifier.fillMaxWidth().padding(2.dp)
    )
}
