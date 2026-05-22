package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

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
import javax.swing.SwingUtilities

class MCResourcePreviewEditor(
    private val project: Project,
    private val file: VirtualFile
) : FileEditor {

    private val userDataHolder = UserDataHolderBase()
    private val changeSupport = PropertyChangeSupport(this)
    private var showRawJson = false
    private val httpPort: Int

    init {
        httpPort = PreviewHttpServer.start()
    }

    private val browser = JBCefBrowser().apply {
        loadHTML(placeholderHtml("正在连接编辑器..."))
    }

    private val panel = MCResourcePreviewPanel(file) { state ->
        SwingUtilities.invokeLater { updateBrowser(state) }
    }

    fun bindEditor(editor: Editor) {
        panel.bindEditor(editor)
    }

    fun unbindEditor() {
        panel.unbindEditor()
    }

    private fun updateBrowser(state: PreviewState) {
        val html = when (state) {
            is PreviewState.WaitingForEditor -> placeholderHtml("正在连接编辑器...")
            is PreviewState.NotADatapack -> placeholderHtml(
                "此文件不在数据包目录中",
                "文件路径需包含 <code>data/&lt;namespace&gt;/&lt;type&gt;/...</code>"
            )
            is PreviewState.JsonError -> errorHtml(state.message)
            is PreviewState.UnsupportedType -> placeholderHtml(
                "「${state.type.displayName}」暂无可视化支持",
                "已注册类型: ${MCResourceViewer.registeredTypes().joinToString { it.displayName }}"
            )
            is PreviewState.Ready -> {
                if (showRawJson) rawJsonHtml(state.rawText)
                else {
                    val viewer = MCResourceViewer.getViewer(state.type)
                    viewer?.buildHtml(state.json, state.rawText, file.name)
                        ?: placeholderHtml("Viewer 未注册: ${state.type.displayName}")
                }
            }
        }
        browser.loadHTML(html)
        PreviewHttpServer.updateHtml(html)
    }

    fun toggleRawJson() {
        showRawJson = !showRawJson
        panel.toggleRawJson()
    }

    // ── UserDataHolder ──────────────────────────────

    override fun <T : Any> getUserData(key: Key<T>): T? = userDataHolder.getUserData(key)
    override fun <T : Any> putUserData(key: Key<T>, value: T?) = userDataHolder.putUserData(key, value)

    // ── FileEditor ──────────────────────────────────

    override fun getComponent(): JComponent = browser.component
    override fun getPreferredFocusedComponent(): JComponent? = browser.component
    override fun getName(): String = "MC 资源预览 (http://localhost:$httpPort/)"
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
        browser.dispose()
    }

    // ── HTML 模板 ───────────────────────────────────

    companion object {
        /** 从当前 IDE 主题读取颜色 */
        private fun themeColors(): String {
            val bg = javax.swing.UIManager.getColor("EditorPane.background")
                ?: java.awt.Color.WHITE
            val fg = javax.swing.UIManager.getColor("EditorPane.foreground")
                ?: java.awt.Color(0x2c, 0x2c, 0x2c)
            val border = javax.swing.UIManager.getColor("Separator.foreground")
                ?: java.awt.Color(0xe0, 0xe0, 0xe0)
            val accent = javax.swing.UIManager.getColor("Link.activeForeground")
                ?: java.awt.Color(0x4a, 0x90, 0xd9)
            fun hex(c: java.awt.Color) =
                "#%02x%02x%02x".format(c.red, c.green, c.blue)
            return "--bg:${hex(bg)};--fg:${hex(fg)};--border:${hex(border)};--accent:${hex(accent)}"
        }

        private fun baseHtml(body: String): String = """
            <!DOCTYPE html><html><head><meta charset='UTF-8'>
            <style>
              :root{${themeColors()}}
              body{font-family:-apple-system,'Segoe UI',sans-serif;margin:0;padding:16px;
                   color:var(--fg);background:var(--bg)}
              .center{display:flex;flex-direction:column;align-items:center;
                      justify-content:center;height:100vh;text-align:center}
              .center h2{font-size:15px;color:var(--fg);opacity:0.6;font-weight:400;margin:0}
              .center p{font-size:12px;color:var(--fg);opacity:0.4;margin-top:8px}
              .error{color:#c62828;background:#ffebee;border:1px solid #ef9a9a;
                     border-radius:8px;padding:16px;font-size:12px}
              .error h3{font-size:14px;margin:0 0 8px}
              .error pre{margin:0;font-family:'JetBrains Mono','Consolas',monospace;
                         font-size:11px;white-space:pre-wrap}
              pre.raw{font-family:'JetBrains Mono','Consolas',monospace;font-size:11px;
                      white-space:pre-wrap;margin:0;padding:12px;color:#333}
            </style></head><body>$body</body></html>
        """.trimIndent()

        fun placeholderHtml(title: String, subtitle: String? = null): String = baseHtml(
            "<div class='center'><h2>$title</h2>" +
            (subtitle?.let { "<p>$it</p>" } ?: "") +
            "</div>"
        )

        fun errorHtml(message: String): String = baseHtml(
            "<div class='error'><h3>JSON 解析错误</h3><pre>$message</pre></div>"
        )

        fun rawJsonHtml(text: String): String = baseHtml(
            "<pre class='raw'>${text.replace("&", "&amp;").replace("<", "&lt;")}</pre>"
        )
    }
}

data class MCResourcePreviewState(val showRawJson: Boolean? = null) : FileEditorState {
    override fun canBeMergedWith(other: FileEditorState, level: FileEditorStateLevel): Boolean =
        other is MCResourcePreviewState
}
