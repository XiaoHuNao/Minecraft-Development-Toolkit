package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.splitedit

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourcePreviewEditor
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourceType

class MCResourceSplitEditorProvider : TextEditorWithPreviewProvider(
    MCResourcePreviewEditorProvider()
), DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!super.accept(project, file)) return false
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
    }
}
