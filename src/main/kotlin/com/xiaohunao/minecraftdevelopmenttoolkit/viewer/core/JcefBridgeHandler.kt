package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

class JcefBridgeHandler(
    private val browser: JBCefBrowser,
    private val onMessage: (type: String, payload: JsonObject) -> Unit = { _, _ -> }
) {
    private val logger = Logger.getInstance(JcefBridgeHandler::class.java)
    private val gson = Gson()
    private val pendingMessages = mutableListOf<String>()
    private var pageReady = false

    private val jsQuery = JBCefJSQuery.create(browser).also { query ->
        query.addHandler { request ->
            try {
                val json = gson.fromJson(request, JsonObject::class.java)
                val type = json.get("type")?.asString ?: return@addHandler null
                if (type == "BRIDGE_READY") {
                    pageReady = true
                    flushPending()
                } else {
                    val payload = json.getAsJsonObject("payload") ?: JsonObject()
                    onMessage(type, payload)
                }
            } catch (e: Exception) {
                logger.error("Failed to parse message from frontend", e)
            }
            null
        }
    }

    init {
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(b: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    injectBridge()
                    waitForBridgeReady()
                }
            }
        }, browser.cefBrowser)
    }

    private fun injectBridge() {
        val injection = jsQuery.inject("msg")
        val script = """
            (function() {
                if (!window.__MDT_BRIDGE__) {
                    window.__MDT_BRIDGE__ = { receive: function() {} };
                }
                window.__MDT_BRIDGE__.sendToPlugin = function(msg) { $injection };
                window.cefQuery = function(params) {
                    window.__MDT_BRIDGE__.sendToPlugin(params.request);
                };
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
    }

    private fun waitForBridgeReady() {
        val checkScript = """
            (function check() {
                if (window.__MDT_BRIDGE__ && window.__MDT_BRIDGE__.__ready) {
                    window.__MDT_BRIDGE__.sendToPlugin(JSON.stringify({type:'BRIDGE_READY',payload:{},timestamp:Date.now()}));
                } else {
                    setTimeout(check, 50);
                }
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(checkScript, browser.cefBrowser.url, 0)
    }

    fun pushMessage(type: String, payload: Any) {
        val envelope = JsonObject().apply {
            addProperty("type", type)
            add("payload", gson.toJsonTree(payload))
            addProperty("timestamp", System.currentTimeMillis())
        }
        val json = gson.toJson(envelope)
        val escaped = json.replace("\\", "\\\\").replace("'", "\\'")
        val script = "window.__MDT_BRIDGE__?.receive('$escaped')"

        if (pageReady) {
            browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
        } else {
            synchronized(pendingMessages) { pendingMessages.add(script) }
        }
    }

    fun pushDocumentUpdate(text: String, filePath: String, resourceType: String?) {
        val payload = JsonObject().apply {
            addProperty("text", text)
            addProperty("filePath", filePath)
            if (resourceType != null) addProperty("resourceType", resourceType)
        }
        pushMessage("DOCUMENT_UPDATE", payload)
    }

    fun pushTheme(colors: Map<String, String>) {
        pushMessage("THEME_UPDATE", colors)
    }

    fun pushConnectionParams(host: String, port: Int, token: String) {
        val payload = JsonObject().apply {
            addProperty("host", host)
            addProperty("port", port)
            addProperty("token", token)
        }
        pushMessage("CONNECTION_PARAMS", payload)
    }

    private fun flushPending() {
        synchronized(pendingMessages) {
            pendingMessages.forEach { script ->
                browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
            }
            pendingMessages.clear()
        }
    }

    fun dispose() {
        jsQuery.dispose()
    }
}
