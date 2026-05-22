package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.splitedit

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.advancement.ui.AdvancementViewer
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourcePreviewEditor
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourceType
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourceViewer

/**
 * MC 资源分屏编辑器的 [FileEditorProvider]。
 *
 * 继承 [TextEditorWithPreviewProvider] 以复用平台的分屏编辑器基础设施。
 * 当检测到文件位于数据包目录中且属于已知 MC 资源类型时，
 * 创建 [MCResourceSplitEditor]（左侧 JSON Editor + 右侧可视化预览）。
 */
class MCResourceSplitEditorProvider : TextEditorWithPreviewProvider(
    MCResourcePreviewEditorProvider()
), DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!super.accept(project, file)) return false
        // 仅对数据包中的 JSON 文件启用分屏预览
        return MCResourceType.detect(file) != null
    }

    override fun createSplitEditor(
        firstEditor: TextEditor,
        secondEditor: FileEditor
    ): FileEditor {
        return MCResourceSplitEditor(firstEditor, secondEditor as MCResourcePreviewEditor)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        const val EDITOR_TYPE_ID = "mdt-mc-resource-split-editor"

        init {
            // Provider 类加载时即注册 Viewer，避免 ProjectActivity 异步时序问题
            MCResourceViewer.register(AdvancementViewer)
        }
    }
}
