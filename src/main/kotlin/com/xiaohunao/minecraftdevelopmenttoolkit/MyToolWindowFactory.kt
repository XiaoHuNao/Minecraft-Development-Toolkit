package com.xiaohunao.minecraftdevelopmenttoolkit

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import com.xiaohunao.minecraftdevelopmenttoolkit.service.MDTConnectionState
import com.xiaohunao.minecraftdevelopmenttoolkit.settings.MDTSettings
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.JcefBridgeHandler
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourcePreviewEditor
import javax.swing.UIManager

class MyToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val browser = JBCefBrowser()
        val bridge = JcefBridgeHandler(browser) { type, payload ->
            when (type) {
                "VIEWER_READY" -> {}
            }
        }

        val url = resolveToolWindowUrl()
        browser.loadURL(url)

        pushThemeColors(bridge)
        pushConnectionParamsIfAvailable(bridge)

        val mdtConnectionState = MDTConnectionState.getInstance()
        mdtConnectionState.addListener(MDTConnectionState.StateChangeListener {
            val request = mdtConnectionState.consumePendingConnection() ?: return@StateChangeListener
            bridge.pushConnectionParams(request.host, request.port, request.token)
        })

        UIManager.addPropertyChangeListener { evt ->
            if (evt.propertyName == "lookAndFeel") {
                pushThemeColors(bridge)
            }
        }

        val content = ContentFactory.getInstance().createContent(
            browser.component,
            MyMessageBundle.message("tab.game.manager"),
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    private fun pushThemeColors(bridge: JcefBridgeHandler) {
        val bg = UIManager.getColor("EditorPane.background") ?: java.awt.Color.WHITE
        val fg = UIManager.getColor("EditorPane.foreground") ?: java.awt.Color(0x2c, 0x2c, 0x2c)
        val border = UIManager.getColor("Separator.foreground") ?: java.awt.Color(0xe0, 0xe0, 0xe0)
        val accent = UIManager.getColor("Link.activeForeground") ?: java.awt.Color(0x4a, 0x90, 0xd9)
        val cardBg = UIManager.getColor("Panel.background") ?: java.awt.Color(0xf5, 0xf5, 0xf5)
        fun hex(c: java.awt.Color) = "#%02x%02x%02x".format(c.red, c.green, c.blue)
        bridge.pushTheme(mapOf(
            "bg" to hex(bg), "fg" to hex(fg), "border" to hex(border),
            "accent" to hex(accent), "cardBg" to hex(cardBg),
            "cardHover" to hex(cardBg), "muted" to "#999999", "error" to "#c62828"
        ))
    }

    private fun pushConnectionParamsIfAvailable(bridge: JcefBridgeHandler) {
        val settings = MDTSettings.getInstance()
        val state = settings.state ?: return
        val host = state.serverHost
        val port = state.serverPort
        val token = state.authToken
        if (host.isNotBlank() && port > 0) {
            bridge.pushConnectionParams(host, port, token)
        }
    }

    companion object {
        private const val DEV_URL = "http://localhost:5173?mode=toolwindow"

        private fun resolveToolWindowUrl(): String {
            if (System.getProperty("mdt.dev") == "true") return DEV_URL
            return com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.WebDevServer.getUrl("toolwindow")
                ?: DEV_URL
        }
    }
}
