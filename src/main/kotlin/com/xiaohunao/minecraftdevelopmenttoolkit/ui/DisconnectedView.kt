package com.xiaohunao.minecraftdevelopmenttoolkit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaohunao.minecraftdevelopmenttoolkit.settings.MDTSettings
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.OutlinedButton
import com.intellij.icons.AllIcons

@Composable
fun DisconnectedView(
    onConnect: (host: String, port: Int, token: String) -> Unit,
    onOpenConnectDialog: () -> Unit
) {
    val settings = remember { MDTSettings.getInstance().state }
    val recentConnections = remember { settings.recentConnections }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon placeholder — using text until proper icon is set up
        Text(
            text = MyMessageBundle.message("disconnected.icon.text"),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = MyMessageBundle.message("disconnected.title"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = MyMessageBundle.message("disconnected.description"),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Steps
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(MyMessageBundle.message("disconnected.step1"), fontSize = 11.sp)
            Text(MyMessageBundle.message("disconnected.step2"), fontSize = 11.sp)
            Text(MyMessageBundle.message("disconnected.step3"), fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Connect button
        if (recentConnections.isNotEmpty()) {
            val last = recentConnections.first()
            DefaultButton(onClick = {
                onConnect(last.host, last.port, last.token)
            }) {
                Text(MyMessageBundle.message("disconnected.quick.connect", last.host, last.port))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(orientation = Orientation.Horizontal)

            Spacer(modifier = Modifier.height(8.dp))

            // Recent servers list
            Text(MyMessageBundle.message("disconnected.recent.servers"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                items(recentConnections) { conn ->
                    OutlinedButton(onClick = {
                        onConnect(conn.host, conn.port, conn.token)
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("${conn.host}:${conn.port}", fontSize = 11.sp)
                    }
                }
            }
        } else {
            DefaultButton(onClick = onOpenConnectDialog) {
                Text(MyMessageBundle.message("disconnected.connect"))
            }
        }
    }
}
