package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

val ButtonRenderer: @Composable (RendererContext) -> Unit = { context ->
    val text = context.node.props.getString("text", "Button")
    val enabled = context.node.props.getBoolean("enabled", true)
    val variant = context.node.props.getString("variant", "filled")
    val hasOnClick = context.node.events.contains("onClick")

    if (variant == "outlined") {
        OutlinedButton(
            onClick = {
                if (hasOnClick) {
                    context.onEvent(context.node.id, "onClick", null)
                }
            },
            enabled = enabled,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(text)
        }
    } else {
        DefaultButton(
            onClick = {
                if (hasOnClick) {
                    context.onEvent(context.node.id, "onClick", null)
                }
            },
            enabled = enabled,
            modifier = Modifier.padding(2.dp)
        ) {
            Text(text)
        }
    }
}
