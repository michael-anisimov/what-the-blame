package com.github.michaelanisimov.whattheblame.git

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile

data class BlameTarget(
    val file: VirtualFile,
    val startLine: Int,
    val endLine: Int,
    val selectedText: String,
    val language: String,
) {
    val lineCount: Int get() = endLine - startLine + 1

    companion object {
        fun from(editor: Editor, file: VirtualFile): BlameTarget? {
            val sm = editor.selectionModel
            if (!sm.hasSelection()) return null
            val doc = editor.document
            val startOffset = sm.selectionStart
            val endOffset = sm.selectionEnd
            if (startOffset == endOffset) return null
            val startLine = doc.getLineNumber(startOffset) + 1
            val endLineRaw = doc.getLineNumber(endOffset)
            val endLine = (
                if (endOffset == doc.getLineStartOffset(endLineRaw) && endLineRaw > 0) endLineRaw
                else endLineRaw + 1
            )
            return BlameTarget(
                file = file,
                startLine = startLine,
                endLine = maxOf(startLine, endLine),
                selectedText = sm.selectedText.orEmpty(),
                language = file.fileType.name,
            )
        }
    }
}
