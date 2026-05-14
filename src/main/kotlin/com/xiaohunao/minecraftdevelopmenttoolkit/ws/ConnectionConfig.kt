package com.xiaohunao.minecraftdevelopmenttoolkit.ws

data class ConnectionConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8765,
    val token: String = "",
    val reconnectOnDisconnect: Boolean = true
) {
    val wsUrl: String get() = "ws://$host:$port"
}
