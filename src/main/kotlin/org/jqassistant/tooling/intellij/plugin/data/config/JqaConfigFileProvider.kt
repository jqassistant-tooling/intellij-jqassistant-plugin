package org.jqassistant.tooling.intellij.plugin.data.config

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
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
    private val listeners: MutableList<FileChangedListener> = mutableListOf()
    private val listenerTemplates: MutableList<Pair<KClass<FileChangedListener>, (VFileEvent) -> Unit>> =
        mutableListOf()
    private val messageBusConnection: MessageBusConnection = project.messageBus.connect()
    private val configFileListener = ConfigFileListener()

    init {
        configFiles.addAll(fetchFiles())
        println("Initial config files: $configFiles")
    }

    fun getFiles(): List<VirtualFile> {
        return configFiles.toList()
    }

    /** Adds a new listener template to the config files
     * */
    fun addFileChangeListener(listenerKClass: KClass<FileChangedListener>, onEvent: (VFileEvent) -> Unit) {
        listenerTemplates.add(Pair(listenerKClass, onEvent))
        applyListenerTemplate(listenerKClass, onEvent)

    }

    private fun applyListenerTemplate(listenerKClass: KClass<FileChangedListener>, onEvent: (VFileEvent) -> Unit) {
        this.getFiles().forEach { file ->
            val listener = listenerKClass.constructors.first().call(file, onEvent)
            this.listeners.add(listener)
            messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
        }
        println("Updated listeners addFilechangeListener: $listeners")

    }

    /** Adds a new file to the list of config files and updates its listeners
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

        println("new config file: $newFile")
        println("Updated config files: $configFiles")
    }

    private fun fetchFiles(): List<VirtualFile> {
        val yamlFiles = FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.projectScope(project))
        val configFiles = yamlFiles.filter { file ->
            ConfigFileUtils.isJqaConfigFile(file)
        }
        return configFiles
    }

    // Listens for new config files
    inner class ConfigFileListener : BulkFileListener {
        private val messageBusConnection: MessageBusConnection = project.messageBus.connect()

        init {
            messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, this)
        }

        override fun after(events: MutableList<out VFileEvent>) {
            events.forEach { event ->
                val file = event.file ?: return
                if (ConfigFileUtils.isJqaConfigFile(file)) {
                    addFile(file)
                }
            }
        }
    }
}
