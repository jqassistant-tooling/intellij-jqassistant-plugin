package org.jqassistant.tooling.intellij.plugin.editor

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.MyBundle"

/**
 * @see [IntelliJ Platform Plugin SDK: Message Bundle Class](https://plugins.jetbrains.com/docs/intellij/internationalization.html#message-bundle-class)
 */
object MessageBundle {
    private val INSTANCE = DynamicBundle(MessageBundle::class.java, BUNDLE)

    fun message(
        key:
            @PropertyKey(resourceBundle = BUNDLE)
            String,
        vararg params: String?,
    ): @Nls String = INSTANCE.getMessage(key, *params)

    fun lazyMessage(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ): Supplier<@Nls String> = INSTANCE.getLazyMessage(key, *params)
}
