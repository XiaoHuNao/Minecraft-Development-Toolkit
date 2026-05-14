package com.xiaohunao.minecraftdevelopmenttoolkit.render.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiaohunao.minecraftdevelopmenttoolkit.render.*
import org.jetbrains.jewel.ui.component.Text

// TODO: Implement actual Markdown parsing/rendering instead of plain text display
val MarkdownRenderer: @Composable (RendererContext) -> Unit = { context ->
    val content = context.node.props.getString("content", "")
    Text(
        text = content,
        modifier = Modifier.fillMaxWidth().padding(2.dp)
    )
}
