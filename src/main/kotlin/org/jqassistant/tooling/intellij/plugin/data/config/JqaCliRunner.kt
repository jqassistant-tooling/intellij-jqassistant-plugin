package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.Main
import com.buschmais.jqassistant.commandline.task.EffectiveConfigurationTask
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class JqaCliRunner {

    private val jqa = "jqassistant:effective-configuration"
    private val jqaCLI = Main()
    private val task = EffectiveConfigurationTask()


    init {

        val config = runEffectiveConfigurationTask()
        println(config)
    }

    private fun runEffectiveConfigurationTask(): String {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val originalOut = System.out
        /*
                val configuration: CliConfiguration = CliConfiguration().apply {
                    setBaseDirectory("path/to/base/directory")
                    setOutputDirectory("path/to/output/directory")
                    setVerbose(true)
                }// Initialize your CliConfiguration instance here
                val options = Options() // Initialize your Options instance here*/

        try {
            System.setOut(printStream)
            jqaCLI.run(arrayOf("effective-configuration"))
            //task.run(configuration, options)
        } finally {
            System.setOut(originalOut)
        }

        return outputStream.toString()
    }
}
