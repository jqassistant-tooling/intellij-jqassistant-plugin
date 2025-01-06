package org.jqassistant.tooling.intellij.plugin.data.jqa.tasks

import com.buschmais.jqassistant.commandline.task.AbstractRuleTask

abstract class CustomRuleTask<T> : AbstractRuleTask() {
    // allow for a return value
    abstract fun getResult(): T
}
