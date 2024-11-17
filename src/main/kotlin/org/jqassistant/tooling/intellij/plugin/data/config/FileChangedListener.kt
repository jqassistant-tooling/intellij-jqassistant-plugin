package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

// Listens for changes on a specific file
class FileChangedListener(private val file: VirtualFile, private val onEvent: (VFileEvent) -> Unit) :
    BulkFileListener {

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach { event ->
            if (event.file == file) {
                onEvent(event)
                println("FileChangedListener: $event")
            }
        }
    }
}
