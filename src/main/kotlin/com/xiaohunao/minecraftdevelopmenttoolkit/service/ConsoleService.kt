package com.xiaohunao.minecraftdevelopmenttoolkit.service

import androidx.compose.runtime.mutableStateOf
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

    val state = mutableStateOf<Map<String, List<ConsoleEntry>>>(emptyMap())

    fun appendLog(tabId: String, message: Message) {
        val payload = message.payload
        val entries = mutableListOf<ConsoleEntry>()

        // Format 1: {"lines": [{timestamp, level, message}, ...]} — from Message.createConsoleAppend
        if (payload.has("lines") && payload.get("lines").isJsonArray) {
            for (element in payload.getAsJsonArray("lines")) {
                val obj = element.asJsonObject
                entries.add(parseEntry(obj))
            }
        }
        // Format 2: {"entries": [{timestamp, level, message}, ...]}
        else if (payload.has("entries") && payload.get("entries").isJsonArray) {
            for (element in payload.getAsJsonArray("entries")) {
                val obj = element.asJsonObject
                entries.add(parseEntry(obj))
            }
        }
        // Format 3: single entry at top level {timestamp, level, message}
        else if (payload.has("level") && payload.has("message")) {
            entries.add(parseEntry(payload))
        }

        if (entries.isEmpty()) return

        val buffer = buffers.getOrPut(tabId) { mutableListOf() }
        buffer.addAll(entries)

        // Trim to ring buffer size
        if (buffer.size > bufferSize) {
            val excess = buffer.size - bufferSize
            repeat(excess) { buffer.removeAt(0) }
        }

        state.value = state.value + (tabId to buffer.toList())
    }

    fun getEntries(tabId: String, levelFilter: String? = null): List<ConsoleEntry> {
        val entries = state.value[tabId] ?: return emptyList()
        return if (levelFilter == null || levelFilter == "ALL") {
            entries
        } else {
            entries.filter { it.level.equals(levelFilter, ignoreCase = true) }
        }
    }

    fun hasEntries(tabId: String): Boolean {
        return (state.value[tabId]?.size ?: 0) > 0
    }

    fun clear(tabId: String) {
        buffers[tabId]?.clear()
        state.value = state.value + (tabId to emptyList())
    }

    fun clearAll() {
        buffers.clear()
        state.value = emptyMap()
    }

    private fun parseEntry(obj: JsonObject): ConsoleEntry {
        val timestamp = if (obj.has("timestamp")) obj.get("timestamp").asLong else System.currentTimeMillis()
        val level = if (obj.has("level")) obj.get("level").asString.uppercase() else "INFO"
        val message = if (obj.has("message")) obj.get("message").asString else obj.toString()
        return ConsoleEntry(timestamp, level, message)
    }
}
