package com.example.einkarcade.catalog

import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Tile

object LevelSummaryMapper {
    fun map(level: Level): LevelSummary =
        LevelSummary(
            puzzleId = level.puzzleId,
            name = level.name,
            isCompleted = level.isCompleted,
            rating = level.rating,
            isStarred = level.isStarred,
            boardGeometry = mapGeometry(level),
        )

    private fun mapGeometry(level: Level): LevelBoardGeometry {
        val rowCount = level.grid.size
        val columnCount = level.grid.firstOrNull()?.size ?: 0
        val tiles =
            level.grid.flatMap { row ->
                row.map { tile ->
                    when (tile) {
                        Tile.FLOOR -> LevelBoardTile.FLOOR
                        Tile.GOAL -> LevelBoardTile.GOAL
                        Tile.VOID -> LevelBoardTile.VOID
                    }
                }
            }

        return LevelBoardGeometry(
            rowCount = rowCount,
            columnCount = columnCount,
            tiles = tiles,
            player = LevelBoardPoint(level.playerStart.row, level.playerStart.col),
            boxes =
                level.boxPositions
                    .map { LevelBoardPoint(it.row, it.col) }
                    .sortedWith(compareBy({ it.row }, { it.col })),
        )
    }
}
