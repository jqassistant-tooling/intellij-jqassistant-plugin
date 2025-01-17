package org.jqassistant.tooling.intellij.plugin.common

import com.buschmais.jqassistant.core.rule.api.model.Rule
import com.buschmais.jqassistant.core.rule.api.model.RuleSet

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
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
    val pluginClassLoader = javaClass.classLoader
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

object MyVfsUtil {
    /**
     * Tries to resolve a path as relative to a project.
     *
     * Since projects in IntelliJ aren't strictly mappable to the file system, this method might return null for complex project setups.
     */
    fun findFileRelativeToProject(project: Project, relativePath: String?): VirtualFile? {
        val base = project.guessProjectDir() ?: return null
        if (relativePath.isNullOrBlank()) return base
        return base.findFileByRelativePath(relativePath) ?: base
    }
}

fun Project.notifyBalloon(message: String, type: NotificationType = NotificationType.INFORMATION) {
    NotificationGroupManager
        .getInstance()
        .getNotificationGroup("jqassistant.NotificationBalloon")
        .createNotification(message, type)
        .notify(this)
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
