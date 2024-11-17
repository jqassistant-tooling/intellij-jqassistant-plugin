package org.jqassistant.tooling.intellij.plugin.common

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xml.NanoXmlUtil
import com.intellij.util.xml.NanoXmlUtil.ParserStoppedXmlException
import com.jetbrains.rd.util.error
import net.n3.nanoxml.IXMLBuilder
import java.io.IOException
import java.io.Reader
import com.jetbrains.rd.util.getLogger

/**
 * NanoXML Builder to extract the target namespace from the root element of a xml file.
 */
private class TargetNamespaceInfoBuilder : IXMLBuilder {
    var targetNamespace: String? = null

    override fun startBuilding(systemID: String?, lineNr: Int) {}

    override fun newProcessingInstruction(target: String?, reader: Reader?) {}

    override fun startElement(name: String?, nsPrefix: String?, nsURI: String?, systemID: String?, lineNr: Int) {}

    override fun addAttribute(key: String?, nsPrefix: String?, nsURI: String?, value: String?, type: String?) {
        if (key == "targetNamespace") {
            targetNamespace = value
            throw ParserStoppedXmlException.INSTANCE
        }
    }

    override fun elementAttributesProcessed(name: String?, nsPrefix: String?, nsURI: String?) {
        throw ParserStoppedXmlException.INSTANCE
    }

    override fun endElement(name: String?, nsPrefix: String?, nsURI: String?) {}

    override fun addPCData(reader: Reader?, systemID: String?, lineNr: Int) {}

    override fun getResult(): String? {
        return targetNamespace
    }
}

/**
 * Extracts the `targetNamespace` attribute from the root element of a xml file.
 */
fun parseXsdTargetNamespace(file: VirtualFile): String? {
    val builder = TargetNamespaceInfoBuilder()
    val inputStream = try {
        file.inputStream
    } catch (e: IOException) {
        getLogger<TargetNamespaceInfoBuilder>().error(e)
        return null
    }
    NanoXmlUtil.parse(inputStream, builder)
    return builder.targetNamespace
}
