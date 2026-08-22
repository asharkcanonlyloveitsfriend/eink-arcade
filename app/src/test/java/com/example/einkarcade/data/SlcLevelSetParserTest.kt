package com.example.einkarcade.data

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.xml.sax.SAXException

class SlcLevelSetParserTest {
    @Test
    fun `parses the level set title`() {
        val slc =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <SokobanLevels>
              <Title>Example Level Set</Title>
            </SokobanLevels>
            """.trimIndent()

        assertEquals("Example Level Set", SlcLevelSetParser.parse(slc.byteInputStream()).title)
    }

    @Test
    fun `parses level grids into an array of ASCII strings`() {
        val slc =
            """
            <SokobanLevels>
              <Title>Example Level Set</Title>
              <LevelCollection>
                <Level Id="1">
                  <L>#####</L>
                  <L>#@$.#</L>
                  <L>#####</L>
                </Level>
                <Level Id="2">
                  <L>#####</L>
                  <L># @ #</L>
                  <L>#####</L>
                </Level>
              </LevelCollection>
            </SokobanLevels>
            """.trimIndent()

        assertArrayEquals(
            arrayOf("#####\n#@$.#\n#####", "#####\n# @ #\n#####"),
            SlcLevelSetParser.parse(slc.byteInputStream()).levels,
        )
    }

    @Test
    fun `parses an SLC file with schema declarations`() {
        val slc =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <SokobanLevels xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="SokobanLev.xsd">
              <Title>Sasquatch III</Title>
              <Description>A real SLC file may include descriptive metadata.</Description>
              <Email>sasquatch98930@yahoo.com</Email>
              <Url>http://users.bentonrea.com/~sasquatch/sokoban/</Url>
              <LevelCollection Copyright="David W Skinner" MaxWidth="30" MaxHeight="18">
                <Level Id="1" Width="8" Height="3">
                  <L> #######</L>
                  <L> #  @  #</L>
                  <L> #######</L>
                </Level>
              </LevelCollection>
            </SokobanLevels>
            """.trimIndent()

        val parsed = SlcLevelSetParser.parse(slc.byteInputStream())

        assertEquals("Sasquatch III", parsed.title)
        assertArrayEquals(arrayOf(" #######\n #  @  #\n #######"), parsed.levels)
    }

    @Test
    fun `rejects SLC files with an external entity declaration`() {
        val externalEntity = Files.createTempFile("slc-external-entity", ".txt")
        try {
            Files.write(externalEntity, "External title".toByteArray())
            val slc =
                """
                <!DOCTYPE SokobanLevels [
                  <!ENTITY title SYSTEM "${externalEntity.toUri()}">
                ]>
                <SokobanLevels>
                  <Title>&title;</Title>
                </SokobanLevels>
                """.trimIndent()

            assertThrows(SAXException::class.java) {
                SlcLevelSetParser.parse(slc.byteInputStream())
            }
        } finally {
            Files.deleteIfExists(externalEntity)
        }
    }
}
