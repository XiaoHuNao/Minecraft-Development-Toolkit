package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.advancement.ui

import com.google.gson.JsonObject
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourceType
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourceViewer

object AdvancementViewer : MCResourceViewer {

    override val resourceType: MCResourceType = MCResourceType.ADVANCEMENT

    override fun buildHtml(json: JsonObject, rawText: String, fileName: String): String {
        val (bg, fg) = themeColors()
        return """
            <!DOCTYPE html><html><head><meta charset='UTF-8'>
            <style>
              body{font-family:-apple-system,'Segoe UI',sans-serif;margin:0;padding:16px;
                   color:$fg;background:$bg}
              .placeholder{display:flex;flex-direction:column;align-items:center;
                   justify-content:center;height:100vh;text-align:center}
              .placeholder h2{font-size:16px;color:$fg;opacity:0.6;font-weight:400;margin:0}
              .placeholder p{font-size:12px;color:$fg;opacity:0.4;margin-top:8px}
            </style></head><body>
            <div class='placeholder'>
              <h2>${esc(fileName)} — 进度预览</h2>
              <p>可视化渲染待实现</p>
            </div>
            </body></html>
        """.trimIndent()
    }

    private fun themeColors(): Pair<String, String> {
        val bg = javax.swing.UIManager.getColor("EditorPane.background")
            ?: java.awt.Color.WHITE
        val fg = javax.swing.UIManager.getColor("EditorPane.foreground")
            ?: java.awt.Color(0x2c, 0x2c, 0x2c)
        fun hex(c: java.awt.Color) = "#%02x%02x%02x".format(c.red, c.green, c.blue)
        return hex(bg) to hex(fg)
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
