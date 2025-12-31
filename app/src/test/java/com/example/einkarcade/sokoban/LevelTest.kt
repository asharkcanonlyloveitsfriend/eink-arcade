package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LevelTest {

    @Test
    fun testFromAscii_RectangularLevelHasNoExteriorMasking() {
        val ascii = """
            #######
            #@ $. #
            #   . #
            #######
        """.trimIndent()

        val level = Level.fromAscii("Rectangular", ascii)

        assertEquals(4, level.grid.size)
        assertEquals(7, level.grid.first().size)
        assertFalse(level.grid.any { row -> row.any { it == Tile.EMPTY } })
        assertEquals(Position(1, 1), level.playerStart)
        assertEquals(setOf(Position(1, 3)), level.boxPositions)
    }

    @Test
    fun testFromAscii_OddBoundaryMasksExteriorFloors() {
        val ascii = """
            #######
            #@  ###
            #######
            #######
            ###
        """.trimIndent()

        val level = Level.fromAscii("OddBoundary", ascii)

        assertEquals(5, level.grid.size)
        assertEquals(7, level.grid.first().size)
        assertEquals(Tile.FLOOR, level.grid[1][2])
        assertEquals(Tile.EMPTY, level.grid[4][3])
        assertEquals(Tile.EMPTY, level.grid[4][6])
    }
}
