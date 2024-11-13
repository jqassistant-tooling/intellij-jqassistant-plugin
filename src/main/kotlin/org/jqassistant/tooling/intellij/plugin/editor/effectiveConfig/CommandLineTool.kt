package org.jqassistant.tooling.intellij.plugin.editor.effectiveConfig

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

// TODO - the ProcessBuilder running the maven goal will be replaced by jqa api. This will NOT be the permanent solution and file location

class CommandLineTool {
    companion object {
        fun runMavenGoal(goal: String, directory: File): String {
            var result = ""
            val mvn = getMavenCmdPath()?.path ?: ""
            val processBuilder = ProcessBuilder(listOf(mvn, goal))
            processBuilder.redirectErrorStream(true)
            processBuilder.directory(directory)
            try {
                val process = processBuilder.start()
                process.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { line ->
                        result += line + "\n"
                    }
                }
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        private fun getMavenCmdPath() : File? {
            var result : File? = null
            val system = System.getProperty("os.name")

            if (system.contains("Linux")){
                result = getLinuxMavenPath()
            } else if (system.contains("Windows")){
                result = getWindowsMavenPath()
            }
            return result
        }

        private fun getWindowsMavenPath() : File {
            val mavenHome = System.getenv("MAVEN_HOME")
            val mvnCommand = "$mavenHome\\bin\\mvn.cmd"
            return File(mvnCommand)
        }

        private fun getLinuxMavenPath() : File {
            val whichProcess = ProcessBuilder("which", "mvn").start()
            val reader = BufferedReader(InputStreamReader(whichProcess.inputStream))
            val mvnPath = reader.readLine()
            return File(mvnPath)
        }
    }
}