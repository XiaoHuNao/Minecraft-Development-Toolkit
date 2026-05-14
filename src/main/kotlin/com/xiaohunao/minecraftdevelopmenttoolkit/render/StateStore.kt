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
        state = newState.deepCopy()
        version = newVersion
        notifyAllListeners()
    }

    fun applyPatch(patch: StatePatch): Boolean {
        if (patch.baseVersion != version) {
            return false // Version conflict, need full snapshot
        }

        for (op in patch.patches) {
            try {
                applyOperation(op.op, op.path, op.value)
            } catch (e: Exception) {
                Logger.getInstance(StateStore::class.java).warn("Failed to apply patch: ${e.message}", e)
                return false // Patch application failed
            }
        }
        version++
        notifyAllListeners()
        return true
    }

    private fun applyOperation(op: String, path: String, value: JsonElement?) {
        val segments = path.trimStart('/').split("/")
        if (segments.isEmpty() || segments[0].isEmpty()) return

        when (op) {
            "replace" -> setAtPath(segments, value)
            "add" -> addAtPath(segments, value)
            "remove" -> removeAtPath(segments)
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
            for (listener in pathListeners) {
                try { listener(value) } catch (_: Exception) {}
            }
        }
    }
}
