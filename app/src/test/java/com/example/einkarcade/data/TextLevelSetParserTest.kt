package com.example.einkarcade.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TextLevelSetParserTest {
    @Test
    fun `parses a TXT title and board-row runs with at least three rows`() {
        val txt =
            """
            Title: Sample Handmade Set
            Description: A test set.

            #####
            #@$.#
            #####
            Title: Level One

            ##
            #@
            Title: Not a level because it only has two rows

            ####
            #_+#
            ####
            """.trimIndent()

        val parsed = TextLevelSetParser.parse(txt.byteInputStream())

        assertEquals("Sample Handmade Set", parsed.title)
        assertArrayEquals(
            arrayOf("#####\n#@$.#\n#####", "####\n#_+#\n####"),
            parsed.levels,
        )
    }

    @Test
    fun `uses the first set or title label it finds`() {
        val sok =
            """
            Date of Last Change:
            Title: First Title

            Set:       Sample Handmade Set
            Author: Example Author

            1
            #####
            #@$.#
            #####
            Title: Level One
            """.trimIndent()

        val parsed = TextLevelSetParser.parse(sok.byteInputStream())

        assertEquals("First Title", parsed.title)
        assertArrayEquals(arrayOf("#####\n#@$.#\n#####"), parsed.levels)
    }

    @Test
    fun `uses a set label when it comes first`() {
        val text =
            """
            Set: First Set
            Title: Later Title

            #####
            #@$.#
            #####
            """.trimIndent()

        val parsed = TextLevelSetParser.parse(text.byteInputStream())

        assertEquals("First Set", parsed.title)
    }

    @Test
    fun `separates levels with a whitespace-only line`() {
        val text =
            listOf(
                "Title: Two Levels",
                "",
                "#####",
                "#@$.#",
                "#####",
                "   ",
                "#####",
                "#@$.#",
                "#####",
            ).joinToString("\n")

        val parsed = TextLevelSetParser.parse(text.byteInputStream())

        assertArrayEquals(
            arrayOf("#####\n#@$.#\n#####", "#####\n#@$.#\n#####"),
            parsed.levels,
        )
    }
}
