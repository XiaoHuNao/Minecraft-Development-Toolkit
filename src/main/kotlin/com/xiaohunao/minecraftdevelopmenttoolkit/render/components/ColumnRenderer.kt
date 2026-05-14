package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*

val ColumnRenderer: @Composable (RendererContext) -> Unit = { context ->
    val padding = context.node.props.getInt("padding", 0)
    val spacing = context.node.props.getInt("spacing", 4)
    val scrollable = context.node.props.getBoolean("scrollable", false)
    val fillMaxHeight = context.node.props.getBoolean("fillMaxHeight", false)

    var modifier = Modifier.fillMaxWidth()
    if (fillMaxHeight) modifier = modifier.fillMaxHeight()
    if (padding > 0) modifier = modifier.padding(padding.dp)
    if (scrollable) modifier = modifier.verticalScroll(rememberScrollState())

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) {
        context.RenderChildren()
    }
}
