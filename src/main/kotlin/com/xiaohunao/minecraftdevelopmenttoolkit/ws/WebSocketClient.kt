package com.xiaohunao.minecraftdevelopmenttoolkit.ws

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import com.xiaohunao.mdt.protocol.ConnectionState
import com.xiaohunao.mdt.protocol.Message
import com.xiaohunao.mdt.protocol.MessageType
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class WebSocketClient(
    private val config: ConnectionConfig,
    private val onMessage: (Message) -> Unit,
    private val onStateChange: (ConnectionState) -> Unit
) {
    private val logger = Logger.getInstance(WebSocketClient::class.java)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val state = AtomicReference(ConnectionState.DISCONNECTED)
    private val reconnectStrategy = ReconnectStrategy()
    private var webSocket: WebSocket? = null
    private var heartbeatFuture: java.util.concurrent.Future<*>? = null
    private var handshakeTimeoutFuture: java.util.concurrent.Future<*>? = null
    private val offlineQueue = mutableListOf<String>()
    private var isUserInitiatedDisconnect = false

    fun connect() {
        val current = state.get()
        if (current == ConnectionState.CONNECTED || current == ConnectionState.HANDSHAKING) {
            return
        }
        if (current != ConnectionState.DISCONNECTED && current != ConnectionState.RECONNECTING) return

        setState(ConnectionState.CONNECTING)
        val request = Request.Builder()
            .url(config.wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                logger.info("WebSocket connected to ${config.wsUrl}")
                setState(ConnectionState.HANDSHAKING)
                reconnectStrategy.reset()
                sendReady()
                startHandshakeTimeout()
                startHeartbeat()
                flushOfflineQueue()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = Message.fromJson(text)
                    when (message.type) {
                        MessageType.HELLO -> {
                            cancelHandshakeTimeout()
                            setState(ConnectionState.CONNECTED)
                            logger.info("Handshake complete, server accepted")
                            onMessage(message)
                        }
                        MessageType.PING -> {
                            val pingTimestamp = message.payload.get("timestamp")?.asLong ?: 0L
                            sendPong(pingTimestamp)
                        }
                        MessageType.ERROR -> {
                            val code = message.payload.get("code")?.asString
                            if (code == "AUTH_FAILED") {
                                logger.error("Authentication failed: server rejected token")
                                disconnect()
                            } else {
                                onMessage(message)
                            }
                        }
                        else -> {
                            if (message.type == MessageType.COMMAND_SUGGEST_RESPONSE) {
                                logger.info("WS received COMMAND_SUGGEST_RESPONSE")
                            }
                            onMessage(message)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to parse message: $text", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logger.info("WebSocket closed: $code $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                logger.error("WebSocket failure", t)
                handleDisconnect()
            }
        })
    }

    private fun sendReady() {
        val supportedComponents = arrayOf(
            "column", "row", "text", "button", "textField", "checkbox",
            "dropdown", "lazyColumn", "progressBar", "divider", "spacer",
            "card", "iconButton", "textArea", "slider", "tabStrip",
            "icon", "banner", "markdown", "table", "tree", "toolbarDecorator"
        )
        val readyMsg = Message.createReady("1.0.0", supportedComponents, config.token)
        send(readyMsg)
    }

    private fun sendPong(pingTimestamp: Long = 0L) {
        val msg = Message.createPong(pingTimestamp)
        send(msg)
    }

    fun send(message: Message) {
        val json = message.toJson()
        val ws = webSocket
        val currentState = state.get()
        if (ws != null && (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.HANDSHAKING)) {
            ws.send(json)
        } else {
            synchronized(offlineQueue) {
                offlineQueue.add(json)
            }
        }
    }

    private fun flushOfflineQueue() {
        val ws = webSocket ?: return
        synchronized(offlineQueue) {
            offlineQueue.forEach { ws.send(it) }
            offlineQueue.clear()
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        heartbeatFuture = scheduler.scheduleWithFixedDelay({
            val pingMsg = Message.createPing()
            send(pingMsg)
        }, 30, 30, TimeUnit.SECONDS)
    }

    private fun stopHeartbeat() {
        heartbeatFuture?.let {
            it.cancel(false)
            heartbeatFuture = null
        }
    }

    private fun startHandshakeTimeout() {
        cancelHandshakeTimeout()
        val scheduler = AppExecutorUtil.getAppScheduledExecutorService()
        handshakeTimeoutFuture = scheduler.schedule({
            if (state.get() == ConnectionState.HANDSHAKING) {
                logger.warn("Handshake timeout: no HELLO received within 10s")
                webSocket?.close(4004, "Handshake timeout")
                handleDisconnect()
            }
        }, 10, TimeUnit.SECONDS)
    }

    private fun cancelHandshakeTimeout() {
        handshakeTimeoutFuture?.let {
            it.cancel(false)
            handshakeTimeoutFuture = null
        }
    }

    private fun handleDisconnect() {
        if (isUserInitiatedDisconnect) {
            isUserInitiatedDisconnect = false
            return
        }
        stopHeartbeat()
        cancelHandshakeTimeout()
        webSocket = null

        if (config.reconnectOnDisconnect && state.get() != ConnectionState.DISCONNECTED) {
            setState(ConnectionState.RECONNECTING)
            scheduleReconnect()
        } else {
            setState(ConnectionState.DISCONNECTED)
        }
    }

    private fun scheduleReconnect() {
        val delay = reconnectStrategy.nextDelay()
        logger.info("Reconnecting in ${delay}ms (attempt ${reconnectStrategy.getAttempt()})")
        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            connect()
        }, delay, TimeUnit.MILLISECONDS)
    }

    fun disconnect() {
        isUserInitiatedDisconnect = true
        setState(ConnectionState.DISCONNECTED)
        stopHeartbeat()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun getState(): ConnectionState = state.get()

    private fun setState(newState: ConnectionState) {
        val oldState = state.getAndSet(newState)
        if (oldState != newState) {
            logger.info("Connection state: $oldState -> $newState")
            onStateChange(newState)
        }
    }

    fun isConnected(): Boolean = state.get() == ConnectionState.CONNECTED
}
