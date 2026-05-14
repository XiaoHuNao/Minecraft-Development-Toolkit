package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*

val TextRenderer: @Composable (RendererContext) -> Unit = { context ->
    val value = context.node.props.getString("value", "")
    val bold = context.node.props.getBoolean("bold", false)
    val size = context.node.props.getString("size", "body")

    val fontSize = when (size) {
        "h1" -> 20.sp
        "h2" -> 16.sp
        "h3" -> 14.sp
        "caption" -> 11.sp
        else -> 12.sp
    }

    val fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal

    Text(
        text = value,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = Modifier.fillMaxWidth()
    )
}
