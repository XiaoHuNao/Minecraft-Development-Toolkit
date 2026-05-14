package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*

val RowRenderer: @Composable (RendererContext) -> Unit = { context ->
    val spacing = context.node.props.getInt("spacing", 4)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        context.RenderChildren()
    }
}
