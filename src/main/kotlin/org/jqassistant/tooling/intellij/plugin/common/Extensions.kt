package org.jqassistant.tooling.intellij.plugin.common

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    val res = mutableMapOf<K, V>()
    return associateTo(res) { it }
}

/**
 * Use reified overloads instead.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> withServiceLoader(block: () -> T, classLoader: ClassLoader?): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val currentThread = Thread.currentThread()
    val originalClassLoader = currentThread.contextClassLoader
    return try {
        currentThread.contextClassLoader = classLoader
        block()
    } finally {
        currentThread.contextClassLoader = originalClassLoader
    }
}

/**
 * Runs code and replaces the IntelliJ Class loader with the one of [Loader].
 *
 * This is required for code that tries to dynamically load some dependencies e.g. JAXB.
 *
 * @see [IntelliJ Platform Plugin SDK: Using ServiceLoader](https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader)
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified Loader : Any, T> withServiceLoader(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    return withServiceLoader(block, Loader::class.java.classLoader)
}

/**
 * Runs code and replaces the IntelliJ Class loader with the one of [Loader].
 *
 * This is required for code that tries to dynamically load some dependencies e.g. JAXB.
 *
 * @see [IntelliJ Platform Plugin SDK: Using ServiceLoader](https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#using-serviceloader)
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified Loader : Any, T> Loader.withServiceLoader(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    return withServiceLoader(block, Loader::class.java.classLoader)
}
