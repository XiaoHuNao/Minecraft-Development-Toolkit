package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.splitedit

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourcePreviewEditor

/**
 * MC 资源分屏编辑器 — 左侧 JSON 编辑器 + 右侧可视化预览。
 */
class MCResourceSplitEditor(
    textEditor: TextEditor,
    preview: MCResourcePreviewEditor
) : TextEditorWithPreview(
    textEditor,
    preview,
    "MCResourceEditor",
    Layout.SHOW_EDITOR_AND_PREVIEW,
    isVerticalSplit = false
) {
    init {
        preview.bindEditor(myEditor.editor)
    }

    override fun dispose() {
        (myPreview as MCResourcePreviewEditor).unbindEditor()
        super.dispose()
    }
}
