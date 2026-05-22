package com.xiaohunao.minecraftdevelopmenttoolkit.viewer.splitedit

import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.xiaohunao.minecraftdevelopmenttoolkit.viewer.core.MCResourcePreviewEditor

class MCResourcePreviewEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension.equals("json", ignoreCase = true)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return MCResourcePreviewEditor(project, file)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        const val EDITOR_TYPE_ID = "mdt-mc-resource-preview"
    }
}
