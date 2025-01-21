package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile

/**
 * Simple class that allows us to hold the FileChooserDescriptor and the optional flag for each TextFieldWithBrowseButton.
 */

class ValidatorTextFieldWithBrowseButton(
    val descriptor: FileChooserDescriptor,
    private val isOptional: Boolean = true,
) : TextFieldWithBrowseButton() {
    fun validateRelativePath(baseFile: VirtualFile?): Boolean {
        var result = true
        val file = VfsUtil.findRelativeFile(baseFile, text)
        if (text.isEmpty()) {
            result = isOptional
        } else if (this.descriptor.isChooseFiles && this.descriptor.isChooseFolders) {
            result = file?.exists() ?: false
        } else if (this.descriptor.isChooseFiles) {
            result = file?.isFile ?: false
        } else if (this.descriptor.isChooseFolders) {
            result = file?.isDirectory ?: false
        }
        return result
    }
}
