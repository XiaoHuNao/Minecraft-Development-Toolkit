package com.xiaohunao.minecraftdevelopmenttoolkit.tab

import com.intellij.ui.content.Content
import com.xiaohunao.minecraftdevelopmenttoolkit.render.StateStore

class TabContent(
    val tabId: String,
    val content: Content?,
    val stateStore: StateStore = StateStore(tabId)
) {
    fun dispose() {
        stateStore.clearListeners()
    }
}
