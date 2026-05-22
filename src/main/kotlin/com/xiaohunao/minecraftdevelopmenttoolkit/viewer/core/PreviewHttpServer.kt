package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.intellij.openapi.diagnostic.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 内嵌 HTTP 服务器，将预览 HTML 暴露给外部浏览器访问。
 *
 * 浏览器打开 http://localhost:<port>/ 即可实时查看预览渲染效果。
 * 页面内嵌 JS 轮询机制，编辑器修改 JSON 后浏览器自动刷新。
 */
object PreviewHttpServer {

    private val logger = Logger.getInstance(PreviewHttpServer::class.java)
    private var server: HttpServer? = null
    private var port: Int = -1

    /** 当前各 tab 的 HTML 内容 */
    private val htmlCache = ConcurrentHashMap<String, String>()

    /** 自动刷新 JS 代码片段 */
    private val autoRefreshScript = """
<script>
let _ver = 0;
setInterval(async () => {
  try {
    const r = await fetch('/_version');
    const v = await r.text();
    if (+v !== _ver) { _ver = +v; location.reload(); }
  } catch(e) {}
}, 500);
</script>
    """.trimIndent()

    private var version = 0L

    @Synchronized
    fun start(preferredPort: Int = 8766): Int {
        if (server != null) return port

        server = try {
            createAndStart(preferredPort)
        } catch (_: Exception) {
            try { createAndStart(0) } catch (e: Exception) { null }
        }

        if (server != null) {
            port = server!!.address.port
            logger.info("MDT Preview HTTP server started on http://localhost:$port/")
        } else {
            logger.warn("Failed to start MDT Preview HTTP server")
            port = -1
        }
        return port
    }

    private fun createAndStart(bindPort: Int): HttpServer {
        return HttpServer.create(InetSocketAddress(bindPort), 0).apply {
            createContext("/") { exchange ->
                val html = htmlCache[""] ?: placeholderHtml()
                respond(exchange, 200, "text/html; charset=utf-8", injectRefresh(html))
            }
            createContext("/_version") { exchange ->
                respond(exchange, 200, "text/plain", version.toString())
            }
            executor = Executors.newSingleThreadExecutor { r ->
                Thread(r, "MDT-Preview-HTTP").apply { isDaemon = true }
            }
            start()
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        port = -1
        htmlCache.clear()
    }

    fun updateHtml(html: String) {
        htmlCache[""] = html
        version++
    }

    fun getPort(): Int = port
    fun isRunning(): Boolean = server != null

    // ── 内部 ─────────────────────────────────────────

    private fun injectRefresh(html: String): String {
        return html.replace("</head>", "$autoRefreshScript</head>")
    }

    private fun respond(exchange: HttpExchange, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", contentType)
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun placeholderHtml(): String = """
        <!DOCTYPE html><html><head><meta charset='UTF-8'>
        <style>body{display:flex;align-items:center;justify-content:center;
        height:100vh;font-family:sans-serif;color:#999}</style>
        </head><body><p>等待编辑器中打开 MC 资源文件...</p></body></html>
    """.trimIndent()
}
