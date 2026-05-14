package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*

val SpacerRenderer: @Composable (RendererContext) -> Unit = { context ->
    val height = context.node.props.getInt("height", 8)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height.dp))
}
