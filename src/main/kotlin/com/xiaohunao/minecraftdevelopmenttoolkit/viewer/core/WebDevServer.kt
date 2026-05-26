package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.intellij.openapi.diagnostic.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.Executors

object WebDevServer {

    private val logger = Logger.getInstance(WebDevServer::class.java)
    private var server: HttpServer? = null
    private var port: Int = -1

    @Synchronized
    fun start(): Int {
        if (server != null) return port

        val webRoot = WebResourceExtractor.getWebRoot()
        if (webRoot == null) {
            logger.error("Cannot start WebDevServer: web resources not found")
            return -1
        }

        server = try {
            createServer(63342, webRoot)
        } catch (_: Exception) {
            try { createServer(0, webRoot) } catch (e: Exception) {
                logger.error("Failed to start WebDevServer", e)
                null
            }
        }

        port = server?.address?.port ?: -1
        if (port > 0) {
            logger.info("MDT WebDevServer started on http://localhost:$port/")
        }
        return port
    }

    fun getUrl(mode: String): String? {
        val p = start()
        if (p <= 0) return null
        return "http://localhost:$p/index.html?mode=$mode"
    }

    private fun createServer(bindPort: Int, webRoot: File): HttpServer {
        return HttpServer.create(InetSocketAddress("127.0.0.1", bindPort), 0).apply {
            executor = Executors.newFixedThreadPool(2)
            createContext("/") { exchange ->
                val path = exchange.requestURI.path.trimStart('/')
                val file = File(webRoot, if (path.isEmpty()) "index.html" else path)

                if (!file.exists() || !file.isFile) {
                    respond(exchange, 404, "text/plain", "Not Found")
                    return@createContext
                }

                val contentType = guessContentType(file.name)
                val bytes = file.readBytes()
                exchange.responseHeaders.add("Content-Type", contentType)
                exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                exchange.responseHeaders.add("Cache-Control", "no-cache")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
    }

    private fun respond(exchange: HttpExchange, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun guessContentType(name: String): String = when {
        name.endsWith(".html") -> "text/html; charset=utf-8"
        name.endsWith(".js") -> "application/javascript; charset=utf-8"
        name.endsWith(".css") -> "text/css; charset=utf-8"
        name.endsWith(".json") -> "application/json; charset=utf-8"
        name.endsWith(".svg") -> "image/svg+xml"
        name.endsWith(".png") -> "image/png"
        else -> "application/octet-stream"
    }
}
