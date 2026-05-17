package com.xiaohunao.minecraftdevelopmenttoolkit.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.xiaohunao.mdt.protocol.ConnectionState

/**
 * Application-wide connection state holder for the MDT plugin.
 *
 * This service acts as the single source of truth for the WebSocket connection
 * state, server name, and protocol version. Both the StatusBar widget and
 * the ToolWindow read from and write to this service, avoiding direct coupling
 * between the two.
 *
 * Changes are published via [addListener]/[removeListener] so that any
 * component (status bar widgets, tool windows, etc.) can react in real-time.
 *
 * The [pendingConnection] mechanism allows external components (like the
 * StatusBar widget) to request a connection. The ToolWindow listens for
 * pending connections and initiates the actual WebSocket handshake.
 */
@Service(Service.Level.APP)
@State(
    name = "com.xiaohunao.mdt.MDTConnectionState",
    storages = [Storage("mdt-connection-state.xml")]
)
class MDTConnectionState : PersistentStateComponent<MDTConnectionState.Persistent> {

    data class Persistent(
        var lastServerName: String = "",
        var lastProtocolVersion: String = ""
    )

    data class ConnectionRequest(
        val host: String,
        val port: Int,
        val token: String
    )

    private var persistent = Persistent()

    @Volatile
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    @Volatile
    var serverName: String = ""
        private set

    @Volatile
    var protocolVersion: String = ""
        private set

    /**
     * When non-null, an external component has requested a connection.
     * The ToolWindow (or another connection manager) should consume this
     * via [consumePendingConnection] and initiate the WebSocket handshake.
     */
    @Volatile
    var pendingConnection: ConnectionRequest? = null
        private set

    fun interface StateChangeListener {
        fun onConnectionStateChanged()
    }

    private val listeners = mutableListOf<StateChangeListener>()

    fun addListener(listener: StateChangeListener) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeListener(listener: StateChangeListener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun fireStateChanged() {
        val snapshot: List<StateChangeListener>
        synchronized(listeners) { snapshot = listeners.toList() }
        ApplicationManager.getApplication().invokeLater {
            snapshot.forEach { it.onConnectionStateChanged() }
        }
    }

    fun setConnectionState(state: ConnectionState) {
        if (this.connectionState == state && state != ConnectionState.DISCONNECTED) return
        this.connectionState = state
        if (state == ConnectionState.DISCONNECTED) {
            serverName = ""
            protocolVersion = ""
        }
        fireStateChanged()
    }

    fun setServerInfo(name: String, protocolVer: String) {
        this.serverName = name
        this.protocolVersion = protocolVer
        this.persistent.lastServerName = name
        this.persistent.lastProtocolVersion = protocolVer
        this.connectionState = ConnectionState.CONNECTED
        fireStateChanged()
    }

    fun requestConnection(host: String, port: Int, token: String) {
        pendingConnection = ConnectionRequest(host, port, token)
        connectionState = ConnectionState.CONNECTING
        fireStateChanged()
    }

    fun consumePendingConnection(): ConnectionRequest? {
        val request = pendingConnection
        pendingConnection = null
        return request
    }

    fun reset() {
        connectionState = ConnectionState.DISCONNECTED
        serverName = ""
        protocolVersion = ""
        pendingConnection = null
        fireStateChanged()
    }

    override fun getState() = persistent
    override fun loadState(state: Persistent) {
        persistent = state
    }

    companion object {
        fun getInstance(): MDTConnectionState =
            ApplicationManager.getApplication().getService(MDTConnectionState::class.java)
    }
}
