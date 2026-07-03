package com.example.einkarcade.catalog

import com.example.einkarcade.sokoban.Level
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelSummaryMapperTest {
    @Test
    fun mapsMetadataAndBoardGeometry() {
        val level = Level.fromAscii("Mapped", "@ $.\n    ", puzzleId = 42)
        level.setRating(1)
        level.setStarred(true)

        val summary = LevelSummaryMapper.map(level)

        assertEquals(42, summary.puzzleId)
        assertEquals(1, summary.rating)
        assertEquals(true, summary.isStarred)
        assertEquals(2, summary.boardGeometry.rowCount)
        assertEquals(4, summary.boardGeometry.columnCount)
        assertEquals(LevelBoardPoint(0, 0), summary.boardGeometry.player)
        assertEquals(listOf(LevelBoardPoint(0, 2)), summary.boardGeometry.boxes)
        assertEquals(LevelBoardTile.GOAL, summary.boardGeometry.tileAt(0, 3))
    }
}
