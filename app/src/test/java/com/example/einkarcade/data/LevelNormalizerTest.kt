package com.example.einkarcade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelNormalizerTest {
    @Test
    fun `pads short rows with walls to make the grid rectangular`() {
        val level =
            """
            ####
            #  #
            #  ####
            #@$.  #
            #######
            """.trimIndent()

        val expected = listOf("  ###", "  ###", "@$.  ").joinToString("\n")

        assertEquals(expected, LevelNormalizer.normalize(level))
    }

    @Test
    fun `turns exterior spaces into walls`() {
        val level =
            """
            ########__
            #      #__
            #  ### ###
            #  # #   #
            #  ###   #
            #@$.     #
            ##########
            """.trimIndent()

        val expected =
            """
                  ##
              ### ##
              ###   
              ###   
            @$.     
            """.trimIndent()

        assertEquals(expected, LevelNormalizer.normalize(level))
    }

    @Test
    fun `fails when the grid has neither player symbol`() {
        val level =
            """
            #####
            # $.#
            #####
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            LevelNormalizer.normalize(level)
        }
    }
}
