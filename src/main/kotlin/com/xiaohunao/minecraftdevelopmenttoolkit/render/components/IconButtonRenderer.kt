package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text

val IconButtonRenderer: @Composable (RendererContext) -> Unit = { context ->
    val enabled = context.node.props.getBoolean("enabled", true)
    val iconKey = context.node.props.getString("icon", "General.Information")

    IconButton(
        onClick = {
            if (context.node.events?.contains("onClick") == true) {
                context.onEvent(context.node.id, "onClick", null)
            }
        },
        enabled = enabled,
        modifier = Modifier.padding(2.dp)
    ) {
        // TODO: Implement proper icon resolution from iconKey instead of rendering first char placeholder
        Text(iconKey.take(1))
    }
}
