package com.xiaohunao.minecraftdevelopmenttoolkit.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.diagnostic.Logger

object StackTraceNavigator {

    private val logger = Logger.getInstance(StackTraceNavigator::class.java)

    private val STACK_TRACE_PATTERN =
        Regex("""at\s+([\w.$]+)\.(\w+)\(([\w$]+\.java):(\d+)\)""")

    data class StackTraceLink(
        val fullMatch: String,
        val className: String,
        val methodName: String,
        val fileName: String,
        val lineNumber: Int,
        val startIndex: Int,
        val endIndex: Int
    )

    fun parseLinks(message: String): List<StackTraceLink> {
        return STACK_TRACE_PATTERN.findAll(message).map { match ->
            StackTraceLink(
                fullMatch = match.value,
                className = match.groupValues[1],
                methodName = match.groupValues[2],
                fileName = match.groupValues[3],
                lineNumber = match.groupValues[4].toInt(),
                startIndex = match.range.first,
                endIndex = match.range.last + 1
            )
        }.toList()
    }

    fun navigate(project: Project, link: StackTraceLink) {
        try {
            val psiFacade = JavaPsiFacade.getInstance(project)
            val psiClass = psiFacade.findClass(link.className, GlobalSearchScope.allScope(project))

            if (psiClass == null) {
                logger.warn("Class not found: ${link.className}")
                com.intellij.openapi.ui.Messages.showWarningDialog(
                    project,
                    "未找到类 ${link.className} 的源码。\n可能该类不在当前项目的依赖中。",
                    "跳转失败"
                )
                return
            }

            val file = psiClass.containingFile?.virtualFile
            if (file == null) {
                logger.warn("Virtual file not found for class: ${link.className}")
                return
            }

            // OpenFileDescriptor 使用 0-based 行号
            OpenFileDescriptor(project, file, link.lineNumber - 1).navigate(true)
        } catch (e: Exception) {
            logger.error("Failed to navigate to ${link.className}:${link.lineNumber}", e)
        }
    }
}
