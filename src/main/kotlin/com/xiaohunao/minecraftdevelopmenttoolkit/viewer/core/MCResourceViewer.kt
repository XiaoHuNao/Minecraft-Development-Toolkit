package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core

import com.google.gson.JsonObject

/**
 * 资源可视化器接口（JCEF HTML 版本）。
 *
 * 每种 [MCResourceType] 对应一个 Viewer 实现，
 * 负责生成该类型资源的 HTML 可视化页面。
 */
interface MCResourceViewer {

    val resourceType: MCResourceType

    /**
     * 根据解析后的 JSON 生成完整的 HTML 页面字符串。
     */
    fun buildHtml(json: JsonObject, rawText: String, fileName: String): String

    companion object {
        private val viewers = mutableMapOf<MCResourceType, MCResourceViewer>()

        fun register(viewer: MCResourceViewer) {
            viewers[viewer.resourceType] = viewer
        }

        fun getViewer(type: MCResourceType): MCResourceViewer? = viewers[type]
        fun registeredTypes(): Set<MCResourceType> = viewers.keys.toSet()
    }
}
