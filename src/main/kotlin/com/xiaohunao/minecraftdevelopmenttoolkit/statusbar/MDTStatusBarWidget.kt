package com.xiaohunao.minecraftdevelopmenttoolkit.statusbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.ui.UIUtil
import com.xiaohunao.mdt.protocol.ConnectionState
import com.xiaohunao.minecraftdevelopmenttoolkit.service.MDTConnectionState
import com.xiaohunao.minecraftdevelopmenttoolkit.MyMessageBundle
import com.xiaohunao.minecraftdevelopmenttoolkit.ui.ConnectDialog
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel

// ── Widget ──────────────────────────────────────────────────────────────────

/**
 * Status bar widget that displays the current MDT WebSocket connection state.
 *
 * Shows:
 * - "MDT: Disconnected"  (grey icon)
 * - "MDT: Connecting..." (yellow icon)
 * - "MDT: Connected (ServerName)" (green icon)
 * - "MDT: Reconnecting..." (orange icon)
 *
 * Clicking the widget opens the [ConnectDialog].
 * The widget listens to [MDTConnectionState] for real-time updates.
 */
class MDTStatusBarWidget(private val project: Project) : StatusBarWidget, CustomStatusBarWidget {

    private var statusBar: StatusBar? = null

    private val stateService: MDTConnectionState = MDTConnectionState.getInstance()

    private val iconLabel = JLabel().apply {
        isOpaque = false
        horizontalAlignment = JLabel.CENTER
    }
    private val textLabel = JLabel().apply {
        isOpaque = false
    }

    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        add(iconLabel)
        add(textLabel)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                showConnectDialog()
            }
        })
    }

    private val stateListener = MDTConnectionState.StateChangeListener {
        updateWidget()
    }

    // ---- StatusBarWidget ----

    override fun ID(): String = "MDTStatusBarWidget"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        stateService.addListener(stateListener)
        updateWidget()
    }

    override fun dispose() {
        stateService.removeListener(stateListener)
        statusBar = null
    }

    // ---- CustomStatusBarWidget ----

    override fun getComponent() = panel

    // ---- Internal ----

    private fun updateWidget() {
        val state = stateService.connectionState
        val serverName = stateService.serverName

        val (icon, text, tooltip) = when (state) {
            ConnectionState.DISCONNECTED -> {
                Triple(
                    AllIcons.General.InspectionsPause,
                    MyMessageBundle.message("statusbar.disconnected"),
                    MyMessageBundle.message("statusbar.disconnected.tooltip")
                )
            }
            ConnectionState.CONNECTING -> {
                Triple(
                    AllIcons.General.InspectionsWarning,
                    MyMessageBundle.message("statusbar.connecting"),
                    MyMessageBundle.message("statusbar.connecting.tooltip")
                )
            }
            ConnectionState.HANDSHAKING -> {
                Triple(
                    AllIcons.General.InspectionsWarning,
                    MyMessageBundle.message("statusbar.handshaking"),
                    MyMessageBundle.message("statusbar.handshaking.tooltip")
                )
            }
            ConnectionState.CONNECTED -> {
                val display = if (serverName.isNotBlank()) MyMessageBundle.message("statusbar.connected", serverName) else MyMessageBundle.message("statusbar.connected.no.name")
                Triple(
                    AllIcons.General.InspectionsOK,
                    display,
                    buildString {
                        append(MyMessageBundle.message("statusbar.connected.tooltip"))
                        if (serverName.isNotBlank()) append(MyMessageBundle.message("statusbar.connected.to.tooltip", serverName))
                        val protoVer = stateService.protocolVersion
                        if (protoVer.isNotBlank()) append(MyMessageBundle.message("statusbar.protocol.tooltip", protoVer))
                    }
                )
            }
            ConnectionState.RECONNECTING -> {
                Triple(
                    AllIcons.General.Warning,
                    MyMessageBundle.message("statusbar.reconnecting"),
                    MyMessageBundle.message("statusbar.reconnecting.tooltip")
                )
            }
        }

        UIUtil.invokeLaterIfNeeded {
            iconLabel.icon = icon
            textLabel.text = text
            panel.toolTipText = tooltip
        }
    }

    private fun showConnectDialog() {
        val dialog = ConnectDialog(project)
        if (dialog.showAndGet()) {
            // Save settings for next time
            val settings = com.xiaohunao.minecraftdevelopmenttoolkit.settings.MDTSettings.getInstance()
            settings.state.serverHost = dialog.serverHost
            settings.state.serverPort = dialog.serverPort
            settings.addRecentConnection(dialog.serverHost, dialog.serverPort, dialog.authToken)

            // Submit a pending connection request via the shared state service.
            // The ToolWindow picks this up and initiates the WebSocket handshake.
            stateService.requestConnection(dialog.serverHost, dialog.serverPort, dialog.authToken)

            // Open/activate the MDT tool window so the user sees the connection attempt
            val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("MyToolWindow")
            toolWindow?.activate(null)
        }
    }
}

// ── Factory ─────────────────────────────────────────────────────────────────

class MDTStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = "MDTStatusBarWidget"
    override fun getDisplayName() = MyMessageBundle.message("statusbar.factory.display.name")
    override fun isAvailable(project: Project) = true
    override fun createWidget(project: Project) = MDTStatusBarWidget(project)
    override fun canBeEnabledOn(statusBar: StatusBar) = true
}
