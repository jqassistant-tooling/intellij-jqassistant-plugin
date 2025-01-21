package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.isFile
import java.awt.event.ActionListener
import java.io.File

class MyTextFieldWithBrowseButton(
    val descriptor: FileChooserDescriptor,
    private val isOptional: Boolean = true,
) : TextFieldWithBrowseButton() {
    fun validatePath(basePath: String): Boolean {
        var result = true
        val file = File(basePath, text)
        val localFileSystem = LocalFileSystem.getInstance()
        val virtualFile = localFileSystem.findFileByIoFile(file)
        if (text.isEmpty()) {
            result = isOptional
        } else if (this.descriptor.isChooseFiles && this.descriptor.isChooseFolders) {
            result = virtualFile?.exists() ?: false
        } else if (this.descriptor.isChooseFiles) {
            result = virtualFile?.isFile ?: false
        } else if (this.descriptor.isChooseFolders) {
            result = virtualFile?.isDirectory ?: false
        }
        return result
    }

    override fun addActionListener(listener: ActionListener?) {
        super.addActionListener(listener)
    }
}
