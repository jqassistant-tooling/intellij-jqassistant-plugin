package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.application
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.yaml.YAMLFileType
import org.jqassistant.tooling.intellij.plugin.editor.config.ConfigFileUtils


// Holds references and listeners to all jQA config files
class JqaConfigFileProvider(private val project: Project) {
    private val configFiles: MutableList<Document> = mutableListOf()
    private val listeners: MutableList<FileListener> = mutableListOf()
    private val messageBusConnection: MessageBusConnection = application.messageBus.connect()

    init {
        fetchDocuments().forEach {
            document -> addDocument(document)
        }
    }

    fun getDocuments(): List<Document> {
        return configFiles.toList()
    }

    /** Adds a new listener template to the config files
     * */
    fun addFileListener(listener: FileListener) {
        this.listeners.add(listener)
    }

    private fun notifyListeners(event: DocumentEvent?) {
        listeners.forEach { listener ->
            listener.onFileChangeEvent()
        }
    }

    /** Adds a new file to the list of config files and updates its listeners
     * TODO shall be used by the listener that watches for new jQA config files
     * */
    private fun addDocument(document: Document) {
        if (configFiles.contains(document)) return

        // make sure to have no doubled listener
            document.addDocumentListener(object : DocumentListener {
                override fun beforeDocumentChange(event: DocumentEvent) {
                    super.beforeDocumentChange(event)
                    notifyListeners(event)

                }})

        configFiles.add(document)
    }

    /** Fetches all jQA yaml config files in the project
     * */
    private fun fetchDocuments(): List<Document> {
        val documents = mutableListOf<Document>()
        ApplicationManager.getApplication().runReadAction {
            val yamlFiles = FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.projectScope(project))
             yamlFiles.filter { file ->
                ConfigFileUtils.isJqaConfigFile(file)
            }.mapNotNull {file -> FileDocumentManager.getInstance().getDocument(file) }.forEach { document ->
                documents.add(document)
            }
        }

        return documents
    }

    // TODO watch for files that turn into jQA config files
}
