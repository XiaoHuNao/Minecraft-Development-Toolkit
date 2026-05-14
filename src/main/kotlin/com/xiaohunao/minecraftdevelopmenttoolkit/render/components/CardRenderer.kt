package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

val CardRenderer: @Composable (RendererContext) -> Unit = { context ->
    val padding = context.node.props.getInt("padding", 8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2B2B2B))
    ) {
        Column(modifier = Modifier.padding(padding.dp)) {
            context.RenderChildren()
        }
    }
}
