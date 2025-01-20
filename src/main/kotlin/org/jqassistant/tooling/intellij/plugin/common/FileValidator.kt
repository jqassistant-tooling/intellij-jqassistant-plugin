package org.jqassistant.tooling.intellij.plugin.common

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.isFile
import java.io.File

class FileValidator {
    companion object {
        fun isValidFileOrDirectory(path: String): Boolean {
            if (path.isEmpty()) return false
            val file = File(path)
            if (!file.exists()) return false
            val localFileSystem = LocalFileSystem.getInstance()
            val virtualFile = localFileSystem.findFileByIoFile(file)
            return virtualFile != null
        }

        fun isValidFile(text: String): Boolean {
            if (text.isEmpty()) return false
            val file = File(text)
            if (!file.exists()) return false
            val localFileSystem = LocalFileSystem.getInstance()
            val virtualFile = localFileSystem.findFileByIoFile(file)
            return virtualFile?.isFile ?: false
        }

        fun isValidDirectory(text: String): Boolean {
            if (text.isEmpty()) return false
            val file = File(text)
            if (!file.exists()) return false
            val localFileSystem = LocalFileSystem.getInstance()
            val virtualFile = localFileSystem.findFileByIoFile(file)
            return virtualFile?.isDirectory ?: false
        }
    }
}
