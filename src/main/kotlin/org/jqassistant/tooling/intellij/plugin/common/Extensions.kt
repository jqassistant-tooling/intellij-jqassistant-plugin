package org.jqassistant.tooling.intellij.plugin.common

fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    val res = mutableMapOf<K, V>()
    return associateTo(res) { it }
}
