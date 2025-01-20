package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jqassistant.tooling.intellij.plugin.common.FileValidator
import java.awt.event.ActionListener

class MyTextFieldWithBrowseButton(
    val descriptor: FileChooserDescriptor,
    private val isOptional: Boolean = true,
) : TextFieldWithBrowseButton() {
    fun validatePath(basePath: String): Boolean {
        var result = true
        val path = "$basePath/$text"
        if (text.isEmpty()) {
            result = isOptional
        } else if (this.descriptor.isChooseFiles && this.descriptor.isChooseFolders) {
            result = FileValidator.isValidFileOrDirectory(path)
        } else if (this.descriptor.isChooseFiles) {
            result = FileValidator.isValidFile(path)
        } else if (this.descriptor.isChooseFolders) {
            result = FileValidator.isValidDirectory(path)
        }
        return result
    }

    override fun addActionListener(listener: ActionListener?) {
        super.addActionListener(listener)
    }
}
