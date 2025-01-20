package org.jqassistant.tooling.intellij.plugin.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jqassistant.tooling.intellij.plugin.common.FileValidator
import javax.swing.JTextField

class MyTextFieldWithBrowseButton : TextFieldWithBrowseButton() {
    var isOptional = true
    var fileChooserDescriptor: FileChooserDescriptor? = null

    fun validatePath(basePath: String): Boolean {
        var result = true
        val path = "$basePath/$text"
        if (text.isEmpty()) {
            result = isOptional
        } else if (fileChooserDescriptor == null) {
            result = FileValidator.isValidFileOrDirectory(path)
        } else if (fileChooserDescriptor!!.isChooseFiles && fileChooserDescriptor!!.isChooseFolders) {
            result = FileValidator.isValidFileOrDirectory(path)
        } else if (fileChooserDescriptor!!.isChooseFiles) {
            result = FileValidator.isValidFile(path)
        } else if (fileChooserDescriptor!!.isChooseFolders) {
            result = FileValidator.isValidDirectory(path)
        }
        return result
    }

    override fun addBrowseFolderListener(
        title: String?,
        description: String?,
        project: Project?,
        fileChooserDescriptor: FileChooserDescriptor?,
    ) {
        super.addBrowseFolderListener(title, description, project, fileChooserDescriptor)
        this.fileChooserDescriptor = fileChooserDescriptor
    }

    override fun addBrowseFolderListener(
        title: String?,
        description: String?,
        project: Project?,
        fileChooserDescriptor: FileChooserDescriptor?,
        accessor: TextComponentAccessor<in JTextField>?,
    ) {
        super.addBrowseFolderListener(title, description, project, fileChooserDescriptor, accessor)
        this.fileChooserDescriptor = fileChooserDescriptor
    }
}
