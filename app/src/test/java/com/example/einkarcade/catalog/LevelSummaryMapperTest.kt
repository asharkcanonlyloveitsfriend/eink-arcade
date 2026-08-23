package com.example.einkarcade.catalog

import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelSummaryMapperTest {
    @Test
    fun mapsMetadataAndBoardGeometry() {
        val level = Level.fromAscii("Mapped", "@ $.\n    ", puzzleId = 42)

        val summary = LevelSummaryMapper.map(level)

        assertEquals(42, summary.puzzleId)
        assertEquals(2, summary.boardGeometry.tileMap.rowCount)
        assertEquals(4, summary.boardGeometry.tileMap.columnCount)
        assertEquals(Position(0, 0), summary.boardGeometry.player)
        assertEquals(setOf(Position(0, 2)), summary.boardGeometry.boxes)
        assertEquals(Tile.GOAL, summary.boardGeometry.tileMap.tileAt(0, 3))
    }
}
