package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.google.gson.JsonObject
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

/**
 * 预览面板的状态
 */
sealed class PreviewState {
    data object WaitingForEditor : PreviewState()
    data class JsonError(val message: String) : PreviewState()
    data object NotADatapack : PreviewState()
    data class UnsupportedType(val type: MCResourceType) : PreviewState()
    data class Ready(
        val type: MCResourceType,
        val json: JsonObject,
        val rawText: String
    ) : PreviewState()
}

/** 防抖延迟 */
private const val DEBOUNCE_MS = 300L

/**
 * MC 资源预览面板 — 监听左侧编辑器文档变化，解析 JSON，检测类型，
 * 回调 [onStateChanged] 通知外部重建 Swing UI。
 */
class MCResourcePreviewPanel(
    private val file: VirtualFile,
    private val onStateChanged: (PreviewState) -> Unit
) {
    private var editor: Editor? = null
    val currentEditor: Editor? get() = editor
    private var debounceFuture: ScheduledFuture<*>? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    var showRawJson = false
        private set

    fun setShowRawJson(value: Boolean) {
        showRawJson = value
    }

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            scheduleUpdate()
        }
    }

    fun bindEditor(editor: Editor) {
        this.editor = editor
        editor.document.addDocumentListener(documentListener)
        updatePreview(editor.document.text)
    }

    fun unbindEditor() {
        debounceFuture?.cancel(false)
        debounceFuture = null
        editor?.document?.removeDocumentListener(documentListener)
        editor = null
    }

    fun toggleRawJson() {
        showRawJson = !showRawJson
        editor?.document?.text?.let { updatePreview(it) }
    }

    private fun scheduleUpdate() {
        debounceFuture?.cancel(false)
        debounceFuture = scheduler.schedule({
            val text = editor?.document?.text ?: return@schedule
            updatePreview(text)
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS)
    }

    private fun updatePreview(text: String) {
        val json = MCResourceType.tryParseJson(text)
        if (json == null) {
            val errorMsg = try {
                com.google.gson.JsonParser.parseString(text)
                "JSON 必须是对象类型"
            } catch (e: Exception) {
                e.message ?: "未知 JSON 错误"
            }
            onStateChanged(PreviewState.JsonError(errorMsg))
            return
        }

        val type = MCResourceType.detect(file)
        if (type == null) {
            onStateChanged(PreviewState.NotADatapack)
            return
        }

        onStateChanged(PreviewState.Ready(type, json, text))
    }
}
