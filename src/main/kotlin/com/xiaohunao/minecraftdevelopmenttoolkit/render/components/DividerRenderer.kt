package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider

val DividerRenderer: @Composable (RendererContext) -> Unit = { context ->
    Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}
