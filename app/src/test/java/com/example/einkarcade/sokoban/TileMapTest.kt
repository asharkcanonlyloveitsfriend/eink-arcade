package com.example.einkarcade.sokoban

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileMapTest {
    private val tileMap =
        TileMap(
            listOf(
                listOf(Tile.VOID, Tile.FLOOR),
                listOf(Tile.GOAL, Tile.VOID),
            ),
        )

    @Test
    fun reportsDimensionsAndTiles() {
        assertEquals(2, tileMap.rowCount)
        assertEquals(2, tileMap.columnCount)
        assertEquals(Tile.GOAL, tileMap.tileAt(Position(1, 0)))
        assertNull(tileMap.tileAt(-1, 0))
        assertNull(tileMap.tileAt(0, 2))
    }

    @Test
    fun classifiesVoidGoalsAndWalkableTiles() {
        assertTrue(tileMap.isVoid(Position(0, 0)))
        assertTrue(tileMap.isGoal(Position(1, 0)))
        assertTrue(tileMap.isWalkable(Position(0, 1)))
        assertTrue(tileMap.isWalkable(Position(1, 0)))
        assertFalse(tileMap.isWalkable(Position(1, 1)))
    }

    @Test
    fun createsWalkableGridFromTiles() {
        val grid = tileMap.walkableGrid()

        assertArrayEquals(arrayOf(false, true), grid[0])
        assertArrayEquals(arrayOf(true, false), grid[1])
    }
}
