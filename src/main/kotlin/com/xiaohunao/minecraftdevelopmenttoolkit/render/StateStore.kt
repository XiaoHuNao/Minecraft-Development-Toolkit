package com.xiaohunao.minecraftdevelopmenttoolkit.render

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.xiaohunao.mdt.protocol.StatePatch
import java.util.concurrent.ConcurrentHashMap

class StateStore(private val tabId: String) {

    private var state = JsonObject()
    private var version = 0
    private val listeners = ConcurrentHashMap<String, MutableList<(JsonElement?) -> Unit>>()

    fun getState(): JsonObject = state.deepCopy()

    fun getVersion(): Int = version

    fun applySnapshot(newState: JsonObject, newVersion: Int) {
        if (state == newState) {
            version = newVersion
            return // 状态未变化，跳过通知
        }
        state = newState.deepCopy()
        version = newVersion
        notifyAllListeners()
    }

    fun applyPatch(patch: StatePatch): Boolean {
        if (patch.baseVersion != version) {
            return false // Version conflict, need full snapshot
        }

        var hasEffectiveChange = false
        for (op in patch.patches) {
            try {
                val changed = applyOperation(op.op, op.path, op.value)
                if (changed) hasEffectiveChange = true
            } catch (e: Exception) {
                Logger.getInstance(StateStore::class.java).warn("Failed to apply patch: ${e.message}", e)
                return false // Patch application failed
            }
        }
        version++
        if (hasEffectiveChange) {
            notifyAllListeners()
        }
        return true
    }

    private fun applyOperation(op: String, path: String, value: JsonElement?): Boolean {
        val segments = path.trimStart('/').split("/")
        if (segments.isEmpty() || segments[0].isEmpty()) return false

        return when (op) {
            "replace" -> {
                if (hasSameValueAtPath(segments, value)) {
                    false // 值相同，跳过
                } else {
                    setAtPath(segments, value)
                    true
                }
            }
            "add" -> {
                if (hasSameValueAtPath(segments, value)) {
                    false // 值相同，跳过
                } else {
                    addAtPath(segments, value)
                    true
                }
            }
            "remove" -> {
                if (valueExistsAtPath(segments)) {
                    removeAtPath(segments)
                    true
                } else {
                    false // 路径不存在，无需移除
                }
            }
            else -> throw IllegalArgumentException("Unsupported patch operation: ${op}")
        }
    }

    private fun setAtPath(segments: List<String>, value: JsonElement?) {
        var current: JsonObject = state
        for (i in 0 until segments.size - 1) {
            val seg = segments[i]
            if (seg.toIntOrNull() != null) {
                Logger.getInstance(StateStore::class.java)
                    .warn("Array index in path segment '$seg' is not fully supported; treating as object key")
            }
            current = if (current.has(seg)) current.getAsJsonObject(seg) else {
                val newObj = JsonObject()
                current.add(seg, newObj)
                newObj
            }
        }
        val lastSeg = segments.last()
        if (lastSeg.toIntOrNull() != null) {
            Logger.getInstance(StateStore::class.java)
                .warn("Array index in final path segment '$lastSeg' is not fully supported; treating as object key")
        }
        current.add(lastSeg, value)
    }

    private fun addAtPath(segments: List<String>, value: JsonElement?) {
        var current: JsonObject = state
        for (i in 0 until segments.size - 1) {
            val seg = segments[i]
            if (seg.toIntOrNull() != null) {
                Logger.getInstance(StateStore::class.java)
                    .warn("Array index in path segment '$seg' is not fully supported; treating as object key")
            }
            current = if (current.has(seg)) current.getAsJsonObject(seg) else {
                val newObj = JsonObject()
                current.add(seg, newObj)
                newObj
            }
        }
        val lastSeg = segments.last()
        if (lastSeg.toIntOrNull() != null) {
            Logger.getInstance(StateStore::class.java)
                .warn("Array index in final path segment '$lastSeg' is not fully supported; treating as object key")
        }
        if (lastSeg == "-" && current.has(segments[segments.size - 2])) {
            // Append to array - simplified: just add as property
            current.add(lastSeg, value)
        } else {
            current.add(lastSeg, value)
        }
    }

    private fun removeAtPath(segments: List<String>) {
        var current: JsonObject = state
        for (i in 0 until segments.size - 1) {
            val seg = segments[i]
            if (!current.has(seg)) return
            current = current.getAsJsonObject(seg)
        }
        current.remove(segments.last())
    }

    fun getValue(path: String): JsonElement? {
        // Bindings from mod side use dot notation (e.g. "server.onlinePlayers").
        // State patches use JSON Pointer ("/server/onlinePlayers") and are handled in applyOperation.
        val normalized = if (path.startsWith("/")) path.trimStart('/').replace('/', '.') else path
        val segments = normalized.split(".")
        if (segments.isEmpty() || segments[0].isEmpty()) return null
        var current: JsonElement? = state
        for (seg in segments) {
            if (current == null || !current.isJsonObject) return null
            current = current.asJsonObject.get(seg)
        }
        return current
    }

    fun getString(path: String): String? {
        val el = getValue(path)
        return if (el != null && el.isJsonPrimitive) el.asString else null
    }

    fun getBoolean(path: String): Boolean? {
        val el = getValue(path)
        return if (el != null && el.isJsonPrimitive) el.asBoolean else null
    }

    fun getInt(path: String): Int? {
        val el = getValue(path)
        return if (el != null && el.isJsonPrimitive) el.asInt else null
    }

    private fun hasSameValueAtPath(segments: List<String>, value: JsonElement?): Boolean {
        val current = getValueAtPath(segments)
        if (current == null && value == null) return true
        if (current == null || value == null) return false
        return current == value
    }

    private fun valueExistsAtPath(segments: List<String>): Boolean {
        return getValueAtPath(segments) != null
    }

    private fun getValueAtPath(segments: List<String>): JsonElement? {
        var current: JsonElement? = state
        for (seg in segments) {
            if (current == null || !current.isJsonObject) return null
            current = current.asJsonObject.get(seg)
        }
        return current
    }

    fun getDouble(path: String): Double? {
        val el = getValue(path)
        return if (el != null && el.isJsonPrimitive) el.asDouble else null
    }

    fun subscribe(path: String, listener: (JsonElement?) -> Unit) {
        listeners.computeIfAbsent(path) { mutableListOf() }.add(listener)
        // Immediately call with current value
        listener(getValue(path))
    }

    fun unsubscribe(path: String, listener: (JsonElement?) -> Unit) {
        listeners[path]?.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    private fun notifyAllListeners() {
        for ((path, pathListeners) in listeners) {
            val value = getValue(path)
            // Snapshot to avoid ConcurrentModificationException when a
            // DisposableEffect.onDispose unsubscribes during iteration
            val snapshot = pathListeners.toList()
            for (listener in snapshot) {
                try { listener(value) } catch (_: Exception) {}
            }
        }
    }
}
