package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.io.InputStream

object WebResourceExtractor {

    private val logger = Logger.getInstance(WebResourceExtractor::class.java)
    private var extractedDir: File? = null

    private val WEB_FILES = listOf(
        "index.html",
        "assets/index.css",
    )

    @Synchronized
    fun getWebRoot(): File? {
        extractedDir?.let { if (it.exists() && File(it, "index.html").exists()) return it }

        val tempDir = File(System.getProperty("java.io.tmpdir"), "mdt-web-${ProcessHandle.current().pid()}")
        tempDir.mkdirs()

        val classLoader = WebResourceExtractor::class.java.classLoader

        val indexStream = classLoader.getResourceAsStream("web/index.html")
        if (indexStream == null) {
            logger.warn("web/index.html not found in classpath")
            return null
        }
        indexStream.close()

        try {
            extractResource(classLoader, "web/index.html", File(tempDir, "index.html"))

            val assetsDir = File(tempDir, "assets")
            assetsDir.mkdirs()

            val indexHtml = classLoader.getResourceAsStream("web/index.html")!!.bufferedReader().readText()
            val assetPattern = Regex("""(?:src|href)="\./(assets/[^"]+)"""")
            val assetFiles = assetPattern.findAll(indexHtml).map { it.groupValues[1] }.toList()

            for (assetPath in assetFiles) {
                extractResource(classLoader, "web/$assetPath", File(tempDir, assetPath))
            }

            extractedDir = tempDir
            logger.info("Web resources extracted to: ${tempDir.absolutePath}")
            return tempDir
        } catch (e: Exception) {
            logger.error("Failed to extract web resources", e)
            return null
        }
    }

    private fun extractResource(classLoader: ClassLoader, resourcePath: String, target: File) {
        val stream: InputStream = classLoader.getResourceAsStream(resourcePath) ?: run {
            logger.warn("Resource not found: $resourcePath")
            return
        }
        target.parentFile?.mkdirs()
        stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
    }

    fun getIndexUrl(mode: String): String? {
        val root = getWebRoot() ?: return null
        val indexFile = File(root, "index.html")
        if (!indexFile.exists()) return null
        return "${indexFile.toURI()}?mode=$mode"
    }
}
