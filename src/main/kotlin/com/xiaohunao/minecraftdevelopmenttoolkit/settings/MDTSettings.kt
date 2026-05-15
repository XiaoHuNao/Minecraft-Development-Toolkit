package com.xiaohunao.minecraftdevelopmenttoolkit.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.xiaohunao.mdt.MDTSettings",
    storages = [Storage("mdt-settings.xml")]
)
class MDTSettings : PersistentStateComponent<MDTSettings.State> {

    data class RecentConnection(
        var host: String = "",
        var port: Int = 8765,
        var token: String = "",
        var lastConnected: Long = 0
    )

    data class State(
        var serverHost: String = "127.0.0.1",
        var serverPort: Int = 8765,
        var authToken: String = "",
        var reconnectOnDisconnect: Boolean = true,
        var recentConnections: MutableList<RecentConnection> = mutableListOf(),
        var consoleBufferSize: Int = 5000
    )

    private var state = State()

    override fun getState() = state
    override fun loadState(state: State) {
        this.state = state
    }

    fun addRecentConnection(host: String, port: Int, token: String = "") {
        state.recentConnections.removeAll { it.host == host && it.port == port }
        state.recentConnections.add(0, RecentConnection(host, port, token, lastConnected = System.currentTimeMillis()))
        if (state.recentConnections.size > 10) {
            state.recentConnections = state.recentConnections.take(10).toMutableList()
        }
    }

    fun removeRecentConnection(host: String, port: Int) {
        state.recentConnections.removeAll { it.host == host && it.port == port }
    }

    fun clearRecentConnections() {
        state.recentConnections.clear()
    }

    companion object {
        fun getInstance(): MDTSettings = ApplicationManager.getApplication().getService(MDTSettings::class.java)
    }
}
