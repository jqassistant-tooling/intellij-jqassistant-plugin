package org.jqassistant.tooling.intellij.plugin.common

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
