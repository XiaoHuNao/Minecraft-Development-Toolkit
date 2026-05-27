package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

class MCResourcePreviewEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val userDataHolder = UserDataHolderBase()
    private val changeSupport = PropertyChangeSupport(this)
    private val browser = JBCefBrowser()

    private val bridge = JcefBridgeHandler(browser) { type, payload ->
        when (type) {
            "VIEWER_READY" -> pushCurrentDocument()
            "TOGGLE_RAW_JSON" -> {
                showRawJson = !showRawJson
            }
        }
    }

    private var showRawJson = false

    private val panel = MCResourcePreviewPanel(file) { state ->
        when (state) {
            is PreviewState.Ready -> {
                if (state.type == MCResourceType.ADVANCEMENT) {
                    pushAdvancementTree(state.rawText)
                } else {
                    bridge.pushDocumentUpdate(
                        state.rawText,
                        file.path,
                        state.type.name
                    )
                }
            }
            is PreviewState.JsonError -> {
                bridge.pushDocumentUpdate("", file.path, null)
            }
            is PreviewState.NotADatapack -> {
                bridge.pushDocumentUpdate("", file.path, null)
            }
            is PreviewState.UnsupportedType -> {
                bridge.pushDocumentUpdate("", file.path, state.type.name)
            }
            is PreviewState.WaitingForEditor -> {}
        }
    }

    init {
        val url = resolveViewerUrl()
        browser.loadURL(url)
        pushThemeColors()
    }

    fun bindEditor(editor: Editor) {
        panel.bindEditor(editor)
    }

    fun unbindEditor() {
        panel.unbindEditor()
    }

    private fun pushCurrentDocument() {
        val editor = panel.currentEditor ?: return
        val text = editor.document.text
        if (text.isBlank()) return
        val type = MCResourceType.detect(file)
        if (type == MCResourceType.ADVANCEMENT) {
            pushAdvancementTree(text)
        } else {
            bridge.pushDocumentUpdate(text, file.path, type?.name)
        }
    }

    private fun pushAdvancementTree(currentFileText: String) {
        val result = MCResourceType.resolveTypeRoot(file) ?: run {
            bridge.pushDocumentUpdate(currentFileText, file.path, "ADVANCEMENT")
            return
        }
        val (namespace, typeRoot) = result

        val subTreeRoot = findSubTreeRoot(file, typeRoot) ?: run {
            bridge.pushDocumentUpdate(currentFileText, file.path, "ADVANCEMENT")
            return
        }

        val subPath = buildRelativePath(subTreeRoot, typeRoot)
        val advancements = JsonObject()
        collectJsonFiles(subTreeRoot, namespace, subPath, advancements, currentFileText)

        val wrapper = JsonObject().apply {
            add("advancements", advancements)
        }
        bridge.pushDocumentUpdate(wrapper.toString(), file.path, "ADVANCEMENT_TREE")
    }

    private fun findSubTreeRoot(file: VirtualFile, typeRoot: VirtualFile): VirtualFile? {
        var dir = file.parent ?: return null
        while (dir != typeRoot && dir.parent != typeRoot) {
            dir = dir.parent ?: return null
        }
        if (dir == typeRoot) return null
        return dir
    }

    private fun buildRelativePath(dir: VirtualFile, typeRoot: VirtualFile): String {
        val parts = mutableListOf<String>()
        var current = dir
        while (current != typeRoot) {
            parts.add(0, current.name)
            current = current.parent ?: break
        }
        return parts.joinToString("/")
    }

    private fun collectJsonFiles(
        dir: VirtualFile,
        namespace: String,
        prefix: String,
        out: JsonObject,
        currentFileText: String
    ) {
        for (child in dir.children) {
            if (child.isDirectory) {
                val nextPrefix = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                collectJsonFiles(child, namespace, nextPrefix, out, currentFileText)
            } else if (child.extension == "json") {
                val subPath = if (prefix.isEmpty()) child.nameWithoutExtension
                    else "$prefix/${child.nameWithoutExtension}"
                val resourceId = "$namespace:$subPath"
                val text = if (child.path == file.path) currentFileText
                    else String(child.contentsToByteArray(), Charsets.UTF_8)
                try {
                    val json = JsonParser.parseString(text)?.asJsonObject ?: continue
                    out.add(resourceId, json)
                } catch (_: Exception) { }
            }
        }
    }

    private fun pushThemeColors() {
        val bg = javax.swing.UIManager.getColor("EditorPane.background") ?: java.awt.Color.WHITE
        val fg = javax.swing.UIManager.getColor("EditorPane.foreground") ?: java.awt.Color(0x2c, 0x2c, 0x2c)
        val border = javax.swing.UIManager.getColor("Separator.foreground") ?: java.awt.Color(0xe0, 0xe0, 0xe0)
        val accent = javax.swing.UIManager.getColor("Link.activeForeground") ?: java.awt.Color(0x4a, 0x90, 0xd9)
        val cardBg = javax.swing.UIManager.getColor("Panel.background") ?: java.awt.Color(0xf5, 0xf5, 0xf5)
        fun hex(c: java.awt.Color) = "#%02x%02x%02x".format(c.red, c.green, c.blue)
        bridge.pushTheme(mapOf(
            "bg" to hex(bg), "fg" to hex(fg), "border" to hex(border),
            "accent" to hex(accent), "cardBg" to hex(cardBg),
            "cardHover" to hex(cardBg), "muted" to "#999999", "error" to "#c62828"
        ))
    }

    fun toggleRawJson() {
        showRawJson = !showRawJson
        panel.toggleRawJson()
    }

    override fun <T : Any> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)
    override fun <T : Any> putUserData(key: Key<T>, value: T?) = userDataHolder.putUserData(key, value)

    override fun getComponent(): JComponent = browser.component
    override fun getPreferredFocusedComponent(): JComponent? = browser.component
    override fun getName(): String = "MC 资源预览"
    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) {
        if (state is MCResourcePreviewState && state.showRawJson != null) {
            showRawJson = state.showRawJson
            panel.setShowRawJson(state.showRawJson)
        }
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState =
        MCResourcePreviewState(showRawJson = showRawJson)

    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        changeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        changeSupport.removePropertyChangeListener(listener)
    }

    override fun dispose() {
        unbindEditor()
        bridge.dispose()
        browser.dispose()
    }

    companion object {
        private const val DEV_URL = "http://localhost:5173?mode=viewer"

        private fun resolveViewerUrl(): String {
            if (System.getProperty("mdt.dev") == "true") return DEV_URL
            return WebDevServer.getUrl("viewer") ?: DEV_URL
        }
    }
}

data class MCResourcePreviewState(val showRawJson: Boolean? = null) : FileEditorState {
    override fun canBeMergedWith(other: FileEditorState, level: FileEditorStateLevel): Boolean =
        other is MCResourcePreviewState
}
