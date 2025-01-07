package org.jqassistant.tooling.intellij.plugin.common

import java.io.InputStream
import javax.xml.bind.JAXB
import kotlin.reflect.KClass

object JaxbUtil {
    /**
     * Constructs a class instance from a xml source using JAXB.
     *
     * Use inline version instead.
     */
    fun <T : Any> unmarshal(klass: KClass<T>, source: InputStream): T =
        withServiceLoader {
            JAXB.unmarshal(source, klass.java)
        }

    /**
     * Constructs a class instance from a xml source using JAXB.
     */
    inline fun <reified T : Any> unmarshal(source: InputStream): T = unmarshal(T::class, source)
}
