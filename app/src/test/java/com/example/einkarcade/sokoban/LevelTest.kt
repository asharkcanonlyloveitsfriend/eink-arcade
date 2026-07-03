package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTest {
    @Test
    fun testFromAscii_ParsesGridAndExtractsEntities() {
        val ascii =
            """
            #######
            #@ $. #
            #   . #
            #######
            """.trimIndent()

        val level = Level.fromAscii("Rectangular", ascii)

        assertEquals(4, level.grid.size)
        assertEquals(7, level.grid.first().size)
        assertEquals(Position(1, 1), level.playerStart)
        assertEquals(setOf(Position(1, 3)), level.boxPositions)
        assertEquals(Tile.GOAL, level.grid[2][4])
    }

    @Test
    fun fromAsciiParsesPlayerAndBoxOnGoals() {
        val level = Level.fromAscii("On goals", "+*")

        assertEquals(Position(0, 0), level.playerStart)
        assertEquals(setOf(Position(0, 1)), level.boxPositions)
        assertEquals(listOf(Tile.GOAL, Tile.GOAL), level.grid.single())
    }

    @Test
    fun fromAsciiRejectsRaggedRows() {
        assertThrows(IllegalArgumentException::class.java) {
            Level.fromAscii("Ragged", "@\n###")
        }
    }

    @Test
    fun fromAsciiAllowsTrailingNewlines() {
        val level = Level.fromAscii("Trailing newline", "#####\n#@$.#\n#####\r\n")

        assertEquals(3, level.grid.size)
        assertEquals(5, level.grid.first().size)
        assertEquals(Position(1, 1), level.playerStart)
        assertEquals(setOf(Position(1, 2)), level.boxPositions)
    }

    @Test
    fun fromAsciiRequiresAPlayer() {
        assertThrows(IllegalArgumentException::class.java) {
            Level.fromAscii("No player", " $.")
        }
    }

    @Test
    fun ratingAndCompletionStateCanBeUpdated() {
        val level = Level.fromAscii("State", "@")

        assertEquals(1, level.toggleThumbUp())
        assertEquals(0, level.toggleThumbUp())
        assertEquals(-1, level.toggleThumbDown())
        level.setStarred(true)
        level.markCompleted("2026-07-03T10:00:00Z")

        assertTrue(level.isStarred)
        assertTrue(level.isCompleted)
        assertEquals("2026-07-03T10:00:00Z", level.completedAt)

        level.setCompletedAt(null)
        assertFalse(level.isCompleted)
    }
}
