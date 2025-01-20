package org.jqassistant.tooling.intellij.plugin.common

import com.buschmais.jqassistant.core.rule.api.model.Rule
import com.buschmais.jqassistant.core.rule.api.model.RuleSet

fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    val res = mutableMapOf<K, V>()
    return associateTo(res) { it }
}

/**
 * Runs code and replaces the IntelliJ Class loader with the plugin one.
 *
 * This is required for code that tries to dynamically load some dependencies e.g. JAXB.
 *
 * @see [IntelliJ Platform Plugin SDK: Using ServiceLoader](https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader)
 */
fun <T, B : Any> B.withServiceLoader(block: () -> T): T {
    val currentThread = Thread.currentThread()
    val originalClassLoader = currentThread.contextClassLoader
    val pluginClassLoader = javaClass.classLoader
    return try {
        currentThread.contextClassLoader = pluginClassLoader
        block()
    } finally {
        currentThread.contextClassLoader = originalClassLoader
    }
}

/**
 * Returns a concatenated list of all [Rule] elements in this [RuleSet]
 */
fun RuleSet.getAllRules(): List<Rule> {
    val rules = mutableListOf<Rule>()
    rules += groupsBucket.all
    rules += conceptBucket.all
    rules += constraintBucket.all

    return rules
}

/**
 * Returns the first [Rule] with the same id in this [RuleSet]
 * if there ist any
 */
fun RuleSet.findRuleById(id: String): Rule? = getAllRules().find { r -> r.id == id }
