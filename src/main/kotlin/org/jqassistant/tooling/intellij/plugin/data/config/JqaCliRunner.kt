package org.jqassistant.tooling.intellij.plugin.data.config

import com.buschmais.jqassistant.commandline.Main

class JqaCliRunner {
    init {
        val jqa = "jqassistant:effective-configuration"
        val string = Main.main(arrayOf(jqa))

    }
}