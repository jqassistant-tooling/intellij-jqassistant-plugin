package org.jqassistant.tooling.intellij.plugin.common

import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class FileValidator {
    companion object {
        // TODO this functions should be tested
        fun validateFileOrDirectory(path: String): Boolean {
            if (path.isEmpty()) return false
            val file = File(path)
            if (!file.exists()) return false
            val localFileSystem = LocalFileSystem.getInstance()
            val virtualFile = localFileSystem.findFileByIoFile(file)
            return virtualFile != null
        }
    }
}
