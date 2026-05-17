package com.xiaohunao.minecraftdevelopmenttoolkit

import org.jetbrains.jewel.ui.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.awt.SwingPanel
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.xiaohunao.mdt.protocol.*
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.JTextField
import com.xiaohunao.minecraftdevelopmenttoolkit.render.ComponentRegistry
import com.xiaohunao.minecraftdevelopmenttoolkit.render.RenderEngine
import com.xiaohunao.minecraftdevelopmenttoolkit.render.StateStore
import com.xiaohunao.minecraftdevelopmenttoolkit.render.components.*
import com.xiaohunao.minecraftdevelopmenttoolkit.service.ConsoleEntry
import com.xiaohunao.minecraftdevelopmenttoolkit.service.MDTConnectionState
import com.xiaohunao.minecraftdevelopmenttoolkit.service.NotificationService
import com.xiaohunao.minecraftdevelopmenttoolkit.tab.TabContent
import com.xiaohunao.minecraftdevelopmenttoolkit.ui.ConnectDialog
import com.xiaohunao.minecraftdevelopmenttoolkit.ui.DisconnectedView
import com.xiaohunao.minecraftdevelopmenttoolkit.ws.ConnectionConfig
import com.xiaohunao.minecraftdevelopmenttoolkit.ws.MessageDispatcher
import com.xiaohunao.minecraftdevelopmenttoolkit.ws.WebSocketClient
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.styling.LocalDefaultTabStyle

class MyToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val componentRegistry = ComponentRegistry().apply {
            register("column", ColumnRenderer)
            register("row", RowRenderer)
            register("text", TextRenderer)
            register("button", ButtonRenderer)
            register("textField", TextFieldRenderer)
            register("lazyColumn", LazyColumnRenderer)
            register("checkbox", CheckboxRenderer)
            register("dropdown", DropdownRenderer)
            register("progressBar", ProgressBarRenderer)
            register("divider", DividerRenderer)
            register("spacer", SpacerRenderer)
            register("card", CardRenderer)
            register("iconButton", IconButtonRenderer)
            register("textArea", TextAreaRenderer)
            register("slider", SliderRenderer)
            register("tabStrip", TabStripRenderer)
            register("icon", IconRenderer)
            register("banner", BannerRenderer)
            register("markdown", MarkdownRenderer)
            register("table", TableRenderer)
            register("tree", TreeRenderer)
            register("toolbarDecorator", ToolbarDecoratorRenderer)
        }

        val connectionState = mutableStateOf(ConnectionState.DISCONNECTED)
        val serverInfo = mutableStateOf<ServerInfo?>(null)
        val serverTabs = mutableStateOf<List<TabDescriptor>>(emptyList())
        val selectedTabId = mutableStateOf<String?>("console")
        val renderTabs = mutableStateOf<Map<String, @Composable () -> Unit>>(emptyMap())

        val tabContents = mutableMapOf<String, TabContent>()

        val renderEngine = RenderEngine(componentRegistry) { tabId, componentId, eventType, value ->
            wsClient?.send(Message.createEventFire(tabId, componentId, eventType, value))
        }

        val dispatcher = MessageDispatcher()
        val notificationService = NotificationService { wsClient }
        val mdtConnectionState = MDTConnectionState.getInstance()

        // IDEA native ConsoleView for server logs
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

        // Console panel with command input field at the bottom
        val consolePanel = JPanel(BorderLayout())
        consolePanel.add(consoleView.component, BorderLayout.CENTER)

        val commandField = JTextField()
        commandField.toolTipText = "输入指令按 Enter 发送，按 Tab 补全"
        commandField.setFocusTraversalKeysEnabled(false)

        var currentPopup: com.intellij.openapi.ui.popup.JBPopup? = null
        var pendingSuggestRequest: String? = null

        val logger = com.intellij.openapi.diagnostic.Logger.getInstance(MyToolWindowFactory::class.java)

        // Tab completion — send request to server for Brigadier-powered suggestions
        commandField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_TAB) {
                    e.consume()
                    val input = commandField.text
                    val cursor = commandField.caretPosition
                    logger.info("Tab pressed, input='$input', cursor=$cursor, connected=${wsClient?.isConnected()}")
                    if (input.isNotEmpty() && wsClient?.isConnected() == true) {
                        pendingSuggestRequest = input
                        wsClient?.send(Message.createCommandSuggestRequest(input, cursor))
                        logger.info("Sent COMMAND_SUGGEST_REQUEST")
                    }
                }
            }
        })

        // Enter to send
        commandField.addActionListener {
            val command = commandField.text.trim()
            if (command.isNotEmpty()) {
                if (wsClient?.isConnected() == true) {
                    wsClient?.send(Message.createCommandInput(command))
                    consoleView.print("> $command\n", ConsoleViewContentType.NORMAL_OUTPUT)
                } else {
                    consoleView.print("[未连接] 无法发送指令\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
                commandField.text = ""
            }
        }
        consolePanel.add(commandField, BorderLayout.SOUTH)

        dispatcher.onNotification { message ->
            notificationService.handleNotification(message)
        }

        dispatcher.onCommandSuggestResponse { message ->
            val payload = message.payload
            val suggestionsArr = payload.getAsJsonArray("suggestions")
            val suggestions = suggestionsArr?.map { it.asString } ?: emptyList()
            val start = payload.get("start")?.asInt ?: 0
            val length = payload.get("length")?.asInt ?: 0
            logger.warn("[MDT] Received suggest response: ${suggestions.size} items: $suggestions (start=$start, len=$length)")

            if (suggestions.isNotEmpty()) {
                javax.swing.SwingUtilities.invokeLater {
                    consoleView.print("[补全] ${suggestions.joinToString(", ")}\n", ConsoleViewContentType.NORMAL_OUTPUT)
                    currentPopup?.cancel()
                    currentPopup = showCompletionPopup(commandField, suggestions) { selected ->
                        val text = commandField.text
                        val newText = text.substring(0, start.coerceIn(0, text.length)) +
                                selected +
                                text.substring((start + length).coerceIn(0, text.length))
                        commandField.text = newText
                        commandField.caretPosition = (start + selected.length).coerceIn(0, newText.length)
                    }
                }
            }
        }

        dispatcher.onConsoleAppend { _, message ->
            val payload = message.payload
            if (payload.has("lines") && payload.get("lines").isJsonArray) {
                for (element in payload.getAsJsonArray("lines")) {
                    val obj = element.asJsonObject
                    val level = if (obj.has("level")) obj.get("level").asString.uppercase() else "INFO"
                    val msg = if (obj.has("message")) obj.get("message").asString else obj.toString()
                    val contentType = when (level) {
                        "ERROR" -> ConsoleViewContentType.ERROR_OUTPUT
                        "WARN" -> ConsoleViewContentType.LOG_WARNING_OUTPUT
                        else -> ConsoleViewContentType.NORMAL_OUTPUT
                    }
                    consoleView.print("[$level] $msg\n", contentType)
                }
            }
        }

        // Reusable connection helper for toolbar, status bar, and pending requests
        val performConnect: (String, Int, String) -> Unit = { host, port, token ->
            connectToServer(host, port, token, dispatcher, connectionState, serverInfo) {
                serverTabs.value = emptyList()
                selectedTabId.value = "console"
                renderTabs.value = emptyMap()
                tabContents.values.forEach { it.dispose() }
                tabContents.clear()
            }
        }

        // Listen for pending connection requests from the StatusBar widget
        mdtConnectionState.addListener(MDTConnectionState.StateChangeListener {
            val request = mdtConnectionState.consumePendingConnection() ?: return@StateChangeListener
            performConnect(request.host, request.port, request.token)
        })

        dispatcher.onUiRender { tabId, message ->
            val componentsArr = message.payload.getAsJsonArray("components")
            val stateObj = message.payload.getAsJsonObject("state")
            val baseVersion = message.payload.get("baseVersion")?.asInt ?: 1

            val components = componentsArr.map { el ->
                ComponentNode.fromJson(el.asJsonObject)
            }

            val tabContent = tabContents.getOrPut(tabId) {
                TabContent(tabId, null, StateStore(tabId))
            }

            if (stateObj != null) {
                tabContent.stateStore.applySnapshot(stateObj, baseVersion)
            }

            val renderFn = renderEngine.renderTab(tabId, components, tabContent.stateStore)
            renderTabs.value = renderTabs.value + (tabId to renderFn)
        }

        dispatcher.onStateUpdate { tabId, message ->
            val tabContent = tabContents[tabId] ?: return@onStateUpdate
            when (message.type) {
                MessageType.STATE_SNAPSHOT -> {
                    val state = message.payload.getAsJsonObject("state")
                    val version = message.payload.get("baseVersion")?.asInt ?: return@onStateUpdate
                    tabContent.stateStore.applySnapshot(state, version)
                }
                MessageType.STATE_DELTA -> {
                    val patch = StatePatch.fromJson(message.payload)
                    if (!tabContent.stateStore.applyPatch(patch)) {
                        wsClient?.send(Message.createError("VERSION_CONFLICT", "State version mismatch, requesting snapshot"))
                    }
                }
                else -> {}
            }
        }

        dispatcher.onTabChange { message ->
            when (message.type) {
                MessageType.TAB_OPEN -> {
                    val descriptor = TabDescriptor.fromJson(message.payload)
                    if (descriptor.id == "console") return@onTabChange
                    val current = serverTabs.value
                    if (current.none { it.id == descriptor.id }) {
                        serverTabs.value = (current + descriptor).sortedBy { it.order }
                    }
                }
                MessageType.TAB_CLOSE -> {
                    val tabId = message.tabId ?: return@onTabChange
                    serverTabs.value = serverTabs.value.filter { it.id != tabId }
                    tabContents.remove(tabId)?.dispose()
                    renderTabs.value = renderTabs.value - tabId
                    if (selectedTabId.value == tabId) {
                        selectedTabId.value = "console"
                    }
                }
                MessageType.TAB_UPDATE -> {
                    val descriptor = TabDescriptor.fromJson(message.payload)
                    serverTabs.value = serverTabs.value.map {
                        if (it.id == descriptor.id) descriptor else it
                    }
                }
                else -> {}
            }
        }

        // Handle HELLO message — extract initial tab list
        dispatcher.onHello { message ->
            val tabsArr = message.payload.getAsJsonArray("tabs")
            if (tabsArr != null) {
                val tabs = tabsArr.map { TabDescriptor.fromJson(it.asJsonObject) }
                    .filter { it.id != "console" }
                    .sortedBy { it.order }
                serverTabs.value = tabs
            }
            val serverName = message.payload.get("serverName")?.asString
            val protocolVersion = message.payload.get("acceptedProtocol")?.asString
            val info = ServerInfo(serverName ?: MyMessageBundle.message("server.unknown"), protocolVersion ?: "1.0.0")
            serverInfo.value = info
            // Sync server info to application-wide state service
            MDTConnectionState.getInstance().setServerInfo(info.name, info.protocolVersion)
        }

        toolWindow.addComposeTab(
            tabDisplayName = MyMessageBundle.message("tab.game.manager"),
            focusOnClickInside = false
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Toolbar(
                    connectionState = connectionState.value,
                    serverInfo = serverInfo.value,
                    onConnect = performConnect,
                    onDisconnect = {
                        wsClient?.disconnect()
                        wsClient = null
                        connectionState.value = ConnectionState.DISCONNECTED
                        serverInfo.value = null
                        mdtConnectionState.reset()
                        serverTabs.value = emptyList()
                        selectedTabId.value = "console"
                        renderTabs.value = emptyMap()
                        tabContents.values.forEach { it.dispose() }
                        tabContents.clear()
                        consoleView.clear()
                    },
                    onOpenConnectDialog = {
                        val dialog = ConnectDialog(project)
                        if (dialog.showAndGet()) {
                            performConnect(dialog.serverHost, dialog.serverPort, dialog.authToken)
                        }
                    }
                )

                Divider(orientation = Orientation.Horizontal)

                when (connectionState.value) {
                    ConnectionState.DISCONNECTED -> {
                        DisconnectedView(
                            onConnect = performConnect,
                            onOpenConnectDialog = {
                                val dialog = ConnectDialog(project)
                                if (dialog.showAndGet()) {
                                    performConnect(dialog.serverHost, dialog.serverPort, dialog.authToken)
                                }
                            }
                        )
                    }
                    ConnectionState.CONNECTING, ConnectionState.HANDSHAKING -> {
                        ConnectingView()
                    }
                    ConnectionState.RECONNECTING -> {
                        ReconnectingView()
                    }
                    ConnectionState.CONNECTED -> {
                        ConnectedContent(
                            project = project,
                            serverTabs = serverTabs.value,
                            selectedTabId = selectedTabId.value,
                            onTabSelect = { tabId -> selectedTabId.value = tabId },
                            renderTabs = renderTabs.value,
                            consolePanel = consolePanel
                        )
                    }
                }
            }
        }
    }

    companion object {
        private var wsClient: WebSocketClient? = null

        private fun connectToServer(
            host: String, port: Int, token: String,
            dispatcher: MessageDispatcher,
            connectionState: MutableState<ConnectionState>,
            serverInfo: MutableState<ServerInfo?>,
            onConnected: () -> Unit
        ) {
            val mdtState = MDTConnectionState.getInstance()
            val config = ConnectionConfig(host, port, token)
            val client = WebSocketClient(config, { message ->
                dispatcher.dispatch(message)
            }, { state ->
                connectionState.value = state
                // Sync to application-wide state service
                mdtState.setConnectionState(state)
                if (state == ConnectionState.DISCONNECTED) {
                    serverInfo.value = null
                    mdtState.reset()
                }
            })
            client.connect()
            wsClient = client
            onConnected()
        }
    }
}

data class ServerInfo(val name: String, val protocolVersion: String)

// ─────────────────────────────────────────────────────────────

@Composable
private fun Toolbar(
    connectionState: ConnectionState,
    serverInfo: ServerInfo?,
    onConnect: (host: String, port: Int, token: String) -> Unit,
    onDisconnect: () -> Unit,
    onOpenConnectDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    OutlinedButton(onClick = onOpenConnectDialog) {
                        Text(MyMessageBundle.message("toolbar.connect"), fontSize = 12.sp)
                    }
                }
                ConnectionState.CONNECTING, ConnectionState.HANDSHAKING -> {
                    DefaultButton(onClick = {}, enabled = false) {
                        Text(MyMessageBundle.message("toolbar.connecting"), fontSize = 12.sp)
                    }
                }
                ConnectionState.RECONNECTING -> {
                    OutlinedButton(onClick = onDisconnect) {
                        Text(MyMessageBundle.message("toolbar.cancel"), fontSize = 12.sp)
                    }
                }
                ConnectionState.CONNECTED -> {
                    DefaultButton(onClick = onDisconnect) {
                        Text(MyMessageBundle.message("toolbar.disconnect"), fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (color, label) = when (connectionState) {
                ConnectionState.CONNECTED -> Color(0xFF4CAF50) to (serverInfo?.name ?: MyMessageBundle.message("status.connected"))
                ConnectionState.CONNECTING -> Color(0xFFFFC107) to MyMessageBundle.message("status.connecting")
                ConnectionState.HANDSHAKING -> Color(0xFFFFC107) to MyMessageBundle.message("status.handshaking")
                ConnectionState.RECONNECTING -> Color(0xFFFF9800) to MyMessageBundle.message("status.reconnecting")
                ConnectionState.DISCONNECTED -> Color.Gray to MyMessageBundle.message("status.disconnected")
            }
            Box(
                modifier = Modifier.size(8.dp)
                    .background(color, CircleShape)
            )
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ConnectedContent(
    project: Project,
    serverTabs: List<TabDescriptor>,
    selectedTabId: String?,
    onTabSelect: (String) -> Unit,
    renderTabs: Map<String, @Composable () -> Unit>,
    consolePanel: JPanel
) {
    val consoleTitle = MyMessageBundle.message("tab.console")

    Column(modifier = Modifier.fillMaxSize()) {
        if (serverTabs.isNotEmpty()) {
            val tabs = buildList {
                add(TabData.Default(
                    selected = selectedTabId == "console",
                    content = { Text(consoleTitle) },
                    closable = false,
                    onClose = {},
                    onClick = { onTabSelect("console") }
                ))
                serverTabs.forEach { tab ->
                    add(TabData.Default(
                        selected = selectedTabId == tab.id,
                        content = { Text(tab.title) },
                        closable = false,
                        onClose = {},
                        onClick = { onTabSelect(tab.id) }
                    ))
                }
            }
            TabStrip(
                tabs = tabs,
                style = LocalDefaultTabStyle.current,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(orientation = Orientation.Horizontal)
        }

        // 内容区
        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentTabId = selectedTabId) {
                null, "console" -> {
                    SwingPanel(
                        factory = { consolePanel },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    val renderFn = renderTabs[currentTabId]
                    if (renderFn != null) {
                        renderFn()
                    } else {
                        Text(
                            MyMessageBundle.message("loading.tab.content"),
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MyMessageBundle.message("connecting.to.server"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.width(120.dp))
        }
    }
}

@Composable
private fun ReconnectingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MyMessageBundle.message("connection.lost.reconnecting"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.width(120.dp))
        }
    }
}

/**
 * 展示补全建议下拉框。
 */
private fun showCompletionPopup(
    field: JTextField,
    suggestions: List<String>,
    onSelect: (String) -> Unit
): com.intellij.openapi.ui.popup.JBPopup {
    val step = object : BaseListPopupStep<String>("补全", suggestions) {
        override fun getTextFor(value: String): String = value
        override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
            if (selectedValue != null) {
                onSelect(selectedValue)
            }
            return PopupStep.FINAL_CHOICE
        }
    }

    val popup = JBPopupFactory.getInstance().createListPopup(step)
    popup.showUnderneathOf(field)
    return popup
}

