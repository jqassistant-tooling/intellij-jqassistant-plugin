package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.yaml.YAMLFileType
import org.jqassistant.tooling.intellij.plugin.editor.config.ConfigFileUtils
import kotlin.reflect.KClass


// Holds references and listeners to all jQA config files
class JqaConfigFileProvider(private val project: Project) {
    private val configFiles: MutableList<VirtualFile> = mutableListOf()
    private val listeners: MutableList<ConfigBulkFileListener> = mutableListOf()
    private val listenerTemplates: MutableList<Pair<KClass<ConfigBulkFileListener>, (VFileEvent) -> Unit>> =
        mutableListOf()
    private val messageBusConnection: MessageBusConnection = project.messageBus.connect()
    //private val configFileListener = ConfigFileListener()

    init {
        configFiles.addAll(fetchFiles())
    }

    fun getFiles(): List<VirtualFile> {
        return configFiles.toList()
    }

    /** Adds a new listener template to the config files
     * */
    fun addFileChangeListener(listenerKClass: KClass<ConfigBulkFileListener>, onEvent: (VFileEvent) -> Unit) {
        listenerTemplates.add(Pair(listenerKClass, onEvent))
        applyListenerTemplate(listenerKClass, onEvent)

    }

    /** applies the ListenerTemplate to all config files */
    private fun applyListenerTemplate(
        listenerKClass: KClass<ConfigBulkFileListener>,
        onEvent: (VFileEvent) -> Unit
    ) {
        this.getFiles().forEach { file ->
            val listener = listenerKClass.constructors.first().call(file, onEvent)
            this.listeners.add(listener)
            messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
        }

    }

    /** Adds a new file to the list of config files and updates its listeners
     * TODO shall be used by the listener that watches for new jQA config files
     * */
    private fun addFile(newFile: VirtualFile) {
        if (configFiles.contains(newFile)) return

        // make sure to have no doubled listener
        messageBusConnection.disconnect()
        listeners.clear()

        configFiles.add(newFile)
        listenerTemplates.forEach { pair ->
            applyListenerTemplate(pair.first, pair.second)
        }
    }

    /** Fetches all jQA yaml config files in the project
     * */
    private fun fetchFiles(): List<VirtualFile> {
        val foundFiles = mutableListOf<VirtualFile>()
        ApplicationManager.getApplication().runReadAction {
            val yamlFiles = FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.projectScope(project))
            yamlFiles.filter { file ->
                ConfigFileUtils.isJqaConfigFile(file)
            }.forEach {
                foundFiles.add(it)
            }
        }

        return foundFiles
    }

    // TODO watch for files that turn into jQA config files
}
