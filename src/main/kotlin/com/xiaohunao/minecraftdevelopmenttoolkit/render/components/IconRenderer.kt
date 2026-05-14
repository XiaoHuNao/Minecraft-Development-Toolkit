package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.Text

val IconRenderer: @Composable (RendererContext) -> Unit = { context ->
    val iconKey = context.node.props.getString("iconKey", "General.Information")
    val tooltip = context.node.props.getString("tooltip", "")

    // TODO: Implement proper icon resolution from iconKey instead of rendering placeholder text
    Text(
        text = "[$iconKey]",
        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier.padding(2.dp)
    )
}
