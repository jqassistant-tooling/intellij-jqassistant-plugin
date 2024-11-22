package org.jqassistant.tooling.intellij.plugin.data

import com.buschmais.jqassistant.core.report.api.ReportReader
import com.buschmais.jqassistant.core.report.impl.XmlReportPlugin
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jqassistant.schema.report.v2.JqassistantReport
import java.io.File

/** Represents an individual report found in the baseDirectory */
data class FoundReport(val baseDirectory: VirtualFile, val report: JqassistantReport)

@Service(Service.Level.PROJECT)
class ReportProviderService(private val project: Project) {
    /**
     * Returns all found jqassistant reports in the current project
     * A single project can contain multiple baseDirectories and therefore also multiple report xml files
    */
    fun readReports(): List<FoundReport> {
        val buildBranchNumber = ApplicationInfo.getInstance().build.components[0]
        val directoryList = if (buildBranchNumber < 230) {
            project.getBaseDirectories()
        } else {
            if (project.basePath == null) {
                emptySet()
            } else {
                val vFile = LocalFileSystem.getInstance().findFileByPath(project.basePath!!)
                if (vFile == null) emptySet()
                else setOf(vFile)
            }
        }

        return directoryList.mapNotNull { baseDir ->
            // https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html
            val pluginClassLoader = javaClass.classLoader
            val reader = ReportReader(pluginClassLoader)

            val file = getXmlReportPath(baseDir) ?: return@mapNotNull null

            try {
                val report = reader.read(file) ?: return@mapNotNull null

                FoundReport(baseDir, report)
            } catch (e: Exception) {
                thisLogger().error("Error while parsing found report in $baseDir", e)
                null
            }
        }
    }

    /**
     * Constructs the default path to the report xml file from a given base directory
     * This does not work with custom target directories
    */
    private fun getXmlReportPath(dir: VirtualFile): File? {
        // !TODO: Try to use virtual file system
        // !TODO: Use maven to get correct build directory
        // https://github.com/jQAssistant/jqassistant/blob/2e7405df54a63f74d039c95b71b5a0a7431f8be2/maven/src/main/java/com/buschmais/jqassistant/scm/maven/ReportMojo.java
        val selectedXmlReportFile =
            File("${dir.path}/target/jqassistant/${XmlReportPlugin.DEFAULT_XML_REPORT_FILE}");

        // This does not work for some reason
        // val vFile = dir.findFileByRelativePath("target/jqassistant/${XmlReportPlugin.DEFAULT_XML_REPORT_FILE}")

        if (!selectedXmlReportFile.exists() || selectedXmlReportFile.isDirectory) return null

        return selectedXmlReportFile
    }
}