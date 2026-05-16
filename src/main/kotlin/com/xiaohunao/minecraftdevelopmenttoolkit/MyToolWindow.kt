package com.xiaohunao.minecraftdevelopmenttoolkit

import org.jetbrains.jewel.ui.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.xiaohunao.mdt.protocol.*
import com.xiaohunao.minecraftdevelopmenttoolkit.render.ComponentRegistry
import com.xiaohunao.minecraftdevelopmenttoolkit.render.RenderEngine
import com.xiaohunao.minecraftdevelopmenttoolkit.render.StateStore
import com.xiaohunao.minecraftdevelopmenttoolkit.render.components.*
import com.xiaohunao.minecraftdevelopmenttoolkit.service.ConsoleEntry
import com.xiaohunao.minecraftdevelopmenttoolkit.service.ConsoleService
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

        // Shared mutable state
        val connectionState = mutableStateOf(ConnectionState.DISCONNECTED)
        val serverInfo = mutableStateOf<ServerInfo?>(null)
        val serverTabs = mutableStateOf<List<TabDescriptor>>(emptyList())
        val selectedTabId = mutableStateOf<String?>(null)
        val renderTabs = mutableStateOf<Map<String, @Composable () -> Unit>>(emptyMap())

        val tabContents = mutableMapOf<String, TabContent>()

        val renderEngine = RenderEngine(componentRegistry) { tabId, componentId, eventType, value ->
            wsClient?.send(Message.createEventFire(tabId, componentId, eventType, value))
        }

        val dispatcher = MessageDispatcher()
        val notificationService = NotificationService { wsClient }
        val consoleService = ConsoleService()
        val mdtConnectionState = MDTConnectionState.getInstance()

        dispatcher.onNotification { message ->
            notificationService.handleNotification(message)
        }

        dispatcher.onConsoleAppend { tabId, message ->
            consoleService.appendLog(tabId, message)
        }

        // Reusable connection helper for toolbar, status bar, and pending requests
        val performConnect: (String, Int, String) -> Unit = { host, port, token ->
            connectToServer(host, port, token, dispatcher, connectionState, serverInfo) {
                serverTabs.value = emptyList()
                selectedTabId.value = null
                renderTabs.value = emptyMap()
                tabContents.values.forEach { it.dispose() }
                tabContents.clear()
                consoleService.clearAll()
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
                    val current = serverTabs.value
                    if (current.none { it.id == descriptor.id }) {
                        serverTabs.value = (current + descriptor).sortedBy { it.order }
                    }
                    if (selectedTabId.value == null) {
                        selectedTabId.value = descriptor.id
                    }
                }
                MessageType.TAB_CLOSE -> {
                    val tabId = message.tabId ?: return@onTabChange
                    serverTabs.value = serverTabs.value.filter { it.id != tabId }
                    tabContents.remove(tabId)?.dispose()
                    renderTabs.value = renderTabs.value - tabId
                    if (selectedTabId.value == tabId) {
                        selectedTabId.value = serverTabs.value.firstOrNull()?.id
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
                val tabs = tabsArr.map { TabDescriptor.fromJson(it.asJsonObject) }.sortedBy { it.order }
                serverTabs.value = tabs
                if (tabs.isNotEmpty() && selectedTabId.value == null) {
                    selectedTabId.value = tabs.first().id
                }
            }
            val serverName = message.payload.get("serverName")?.asString
            val protocolVersion = message.payload.get("acceptedProtocol")?.asString
            val info = ServerInfo(serverName ?: MyMessageBundle.message("server.unknown"), protocolVersion ?: "1.0.0")
            serverInfo.value = info
            // Sync server info to application-wide state service
            MDTConnectionState.getInstance().setServerInfo(info.name, info.protocolVersion)
        }

        toolWindow.addComposeTab(MyMessageBundle.message("tab.game.manager")) {
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
                        selectedTabId.value = null
                        renderTabs.value = emptyMap()
                        tabContents.values.forEach { it.dispose() }
                        tabContents.clear()
                        consoleService.clearAll()
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
                            serverTabs = serverTabs.value,
                            selectedTabId = selectedTabId.value,
                            onTabSelect = { tabId -> selectedTabId.value = tabId },
                            renderTabs = renderTabs.value,
                            consoleService = consoleService
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

// ── Internal UI Components ──────────────────────────────────────

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
        // Left: connection actions
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

        // Right: server status indicator
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
    serverTabs: List<TabDescriptor>,
    selectedTabId: String?,
    onTabSelect: (String) -> Unit,
    renderTabs: Map<String, @Composable () -> Unit>,
    consoleService: ConsoleService
) {
    if (serverTabs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                MyMessageBundle.message("connected.waiting"),
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Internal tab strip for server-driven tabs
        if (serverTabs.size > 1) {
            val selectedIndex = serverTabs.indexOfFirst { it.id == selectedTabId }
                .coerceIn(0, serverTabs.lastIndex)

            TabStrip(
                tabs = serverTabs.map { tab ->
                    TabData.Default(
                        selected = tab.id == selectedTabId,
                        content = { Text(tab.title) },
                        closable = false,
                        onClose = {},
                        onClick = { onTabSelect(tab.id) }
                    )
                },
                style = LocalDefaultTabStyle.current,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(orientation = Orientation.Horizontal)
        }

        // Tab content area
        val currentTabId = selectedTabId
        if (currentTabId != null) {
            val renderFn = renderTabs[currentTabId]
            val isConsoleTab = currentTabId.equals("console", ignoreCase = true) ||
                consoleService.hasEntries(currentTabId)

            if (isConsoleTab) {
                ConsoleTabContent(
                    consoleService = consoleService,
                    tabId = currentTabId
                )
            } else if (renderFn != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    renderFn()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
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

// ── Console Tab ──────────────────────────────────────────────────

private val LEVEL_COLORS = mapOf(
    "INFO" to Color(0xFF4CAF50),
    "WARN" to Color(0xFFFFC107),
    "ERROR" to Color(0xFFF44336),
    "DEBUG" to Color.Gray
)

private val LEVEL_FILTERS = listOf("ALL", "INFO", "WARN", "ERROR", "DEBUG")

@Composable
private fun ConsoleTabContent(
    consoleService: ConsoleService,
    tabId: String
) {
    var levelFilter by remember { mutableStateOf("ALL") }
    val allEntries = consoleService.state.value[tabId] ?: emptyList()
    val filteredEntries = if (levelFilter == "ALL") allEntries
        else allEntries.filter { it.level.equals(levelFilter, ignoreCase = true) }

    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(filteredEntries.size) {
        if (filteredEntries.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LEVEL_FILTERS.forEach { level ->
                val isSelected = levelFilter == level
                if (isSelected) {
                    DefaultButton(onClick = { levelFilter = level }) {
                        Text(level, fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(onClick = { levelFilter = level }) {
                        Text(level, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(onClick = { consoleService.clear(tabId) }) {
                Text(MyMessageBundle.message("console.clear"), fontSize = 11.sp)
            }
        }

        Divider(orientation = Orientation.Horizontal)

        // Log list
        if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    MyMessageBundle.message("console.no.entries"),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                ) {
                    filteredEntries.forEach { entry ->
                        LogEntryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: ConsoleEntry) {
    val levelColor = LEVEL_COLORS[entry.level] ?: Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timestamp
        Text(
            text = entry.formattedTime,
            fontSize = 11.sp,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Level badge
        Box(
            modifier = Modifier
                .background(levelColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = entry.level,
                fontSize = 10.sp,
                color = levelColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Message
        Text(
            text = entry.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

