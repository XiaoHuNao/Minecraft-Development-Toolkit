package com.xiaohunao.minecraftdevelopmenttoolkit.service

import com.google.gson.JsonObject
import com.xiaohunao.mdt.protocol.Message
import com.xiaohunao.minecraftdevelopmenttoolkit.settings.MDTSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ConsoleEntry(
    val timestamp: Long,
    val level: String,
    val message: String
) {
    val formattedTime: String
        get() = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
}

class ConsoleService {

    private val bufferSize: Int
        get() = MDTSettings.getInstance().state.consoleBufferSize

    private val buffers = mutableMapOf<String, MutableList<ConsoleEntry>>()
    private var entries: Map<String, List<ConsoleEntry>> = emptyMap()

    fun appendLog(tabId: String, message: Message) {
        val payload = message.payload
        val newEntries = mutableListOf<ConsoleEntry>()

        if (payload.has("lines") && payload.get("lines").isJsonArray) {
            for (element in payload.getAsJsonArray("lines")) {
                newEntries.add(parseEntry(element.asJsonObject))
            }
        } else if (payload.has("entries") && payload.get("entries").isJsonArray) {
            for (element in payload.getAsJsonArray("entries")) {
                newEntries.add(parseEntry(element.asJsonObject))
            }
        } else if (payload.has("level") && payload.has("message")) {
            newEntries.add(parseEntry(payload))
        }

        if (newEntries.isEmpty()) return

        val buffer = buffers.getOrPut(tabId) { mutableListOf() }
        buffer.addAll(newEntries)

        if (buffer.size > bufferSize) {
            val excess = buffer.size - bufferSize
            repeat(excess) { buffer.removeAt(0) }
        }

        entries = entries + (tabId to buffer.toList())
    }

    fun getEntries(tabId: String): List<ConsoleEntry> = entries[tabId] ?: emptyList()

    fun clear(tabId: String) {
        buffers[tabId]?.clear()
        entries = entries + (tabId to emptyList())
    }

    fun clearAll() {
        buffers.clear()
        entries = emptyMap()
    }

    private fun parseEntry(obj: JsonObject): ConsoleEntry {
        val timestamp = if (obj.has("timestamp")) obj.get("timestamp").asLong else System.currentTimeMillis()
        val level = if (obj.has("level")) obj.get("level").asString.uppercase() else "INFO"
        val message = if (obj.has("message")) obj.get("message").asString else obj.toString()
        return ConsoleEntry(timestamp, level, message)
    }
}
