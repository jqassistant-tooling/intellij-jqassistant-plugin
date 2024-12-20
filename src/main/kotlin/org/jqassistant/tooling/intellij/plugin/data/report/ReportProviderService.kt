package org.jqassistant.tooling.intellij.plugin.data.report

import com.buschmais.jqassistant.core.report.api.ReportReader
import com.buschmais.jqassistant.core.report.impl.XmlReportPlugin
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jqassistant.schema.report.v2.GroupType
import org.jqassistant.schema.report.v2.JqassistantReport
import org.jqassistant.schema.report.v2.ReferencableRuleType
import java.io.File

@Service(Service.Level.PROJECT)
class ReportProviderService(
    private val project: Project,
) {
    private val cachedReports: MutableMap<VirtualFile, JqassistantReport> = mutableMapOf()

    /**
     * Returns all found jQAssistant reports in the current project, if no reports are cached yet this function will
     * read the reports from all baseDirectories and return them. If the reports have already been read previously in
     * this project, the previously read reports will be returned unless the forceReload parameter is set to true
     *
     * A single project can contain multiple baseDirectories and therefore also multiple report xml files, the returned map
     * will map the VirtualFile of a base directory to it's Report
     */
    @Synchronized // TODO: Investigate Multithreading issues with reading cached report while overwriting the cache
    fun getReports(forceReload: Boolean = false): Map<VirtualFile, JqassistantReport> {
        if (!forceReload && getCachedReports().isNotEmpty()) return cachedReports

        val directoryList = project.getBaseDirectories()
        // Clear reports cache to avoid stale entries
        cachedReports.clear()

        for (baseDir in directoryList) {
            val file = getXmlReportPath(baseDir) ?: continue

            // https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html
            val pluginClassLoader = javaClass.classLoader
            val reader = ReportReader(pluginClassLoader)

            try {
                val report = reader.read(file) ?: continue

                cachedReports[baseDir] = report
            } catch (e: Exception) {
                thisLogger().error("Error while parsing report in $baseDir", e)
                continue
            }
        }

        return cachedReports
    }

    /**
     * This does not read any reports and instead just returns the already read reports even if they're empty
     */
    fun getCachedReports(): Map<VirtualFile, JqassistantReport> = cachedReports

    companion object {
        /**
         * Converts a report to a flat list of all rules
         */
        fun flattenReport(report: JqassistantReport): List<ReferencableRuleType> {
            fun flatMapReportEntry(entry: ReferencableRuleType): List<ReferencableRuleType> =
                listOf(entry) +
                    when (entry) {
                        is GroupType -> entry.groupOrConceptOrConstraint.flatMap { rule -> flatMapReportEntry(rule) }
                        else -> emptyList()
                    }

            return report.groupOrConceptOrConstraint.flatMap { rule ->
                flatMapReportEntry(
                    rule,
                )
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
            File("${dir.path}/target/jqassistant/${XmlReportPlugin.DEFAULT_XML_REPORT_FILE}")

        // This does not work for some reason
        // val vFile = dir.findFileByRelativePath("target/jqassistant/${XmlReportPlugin.DEFAULT_XML_REPORT_FILE}")

        if (!selectedXmlReportFile.exists() || selectedXmlReportFile.isDirectory) return null

        return selectedXmlReportFile
    }
}
