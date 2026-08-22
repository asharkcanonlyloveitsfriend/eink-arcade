package com.example.einkarcade.data

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.SAXException

object SlcLevelSetParser {
    fun parse(input: InputStream): ParsedLevelSet {
        val document = parseDocument(input)
        val title = document.getElementsByTagName("Title").item(0)?.textContent?.trim()

        require(!title.isNullOrEmpty()) { "SLC file does not contain a title." }
        return ParsedLevelSet(title = title, levels = parseLevels(document))
    }

    private fun parseLevels(document: org.w3c.dom.Document): Array<String> {
        val levelNodes = document.getElementsByTagName("Level")

        return Array(levelNodes.length) { index ->
            val lineNodes = levelNodes.item(index).childNodes
            buildList {
                for (lineIndex in 0 until lineNodes.length) {
                    val line = lineNodes.item(lineIndex)
                    if (line.nodeName == "L") {
                        add(line.textContent)
                    }
                }
            }.joinToString("\n")
        }
    }

    private fun parseDocument(input: InputStream) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .apply {
                // Android's DOM factory does not support the standard feature flags for external
                // entities. EntityResolver is invoked before it opens an external entity or DTD.
                setEntityResolver { _, _ ->
                    throw SAXException("External entities are not allowed in SLC files.")
                }
            }.parse(input)

}
