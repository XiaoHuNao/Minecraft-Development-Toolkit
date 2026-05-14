package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.ui.component.Text

val BannerRenderer: @Composable (RendererContext) -> Unit = { context ->
    val text = context.node.props.getString("text", "")
    val type = context.node.props.getString("type", "info")

    val bannerColor = when (type) {
        "success" -> Color(0xFF2E7D32)
        "warning" -> Color(0xFFF9A825)
        "error" -> Color(0xFFC62828)
        else -> Color(0xFF1565C0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .background(bannerColor)
    ) {
        Text(text = text, modifier = Modifier.padding(8.dp))
    }
}
