package org.jqassistant.tooling.intellij.plugin.data.jqa.tasks

import com.buschmais.jqassistant.commandline.configuration.CliConfiguration
import com.buschmais.jqassistant.core.rule.api.model.RuleSet
import org.apache.commons.cli.Options

class EffectiveRulesTask : CustomRuleTask<RuleSet?>() {
    private var rules: RuleSet? = null

    override fun getResult(): RuleSet? = rules

    override fun run(configuration: CliConfiguration, options: Options?) {
        val analyze = configuration.analyze()
        val rule = analyze.rule()
        this.rules = getAvailableRules(rule)
    }
}
