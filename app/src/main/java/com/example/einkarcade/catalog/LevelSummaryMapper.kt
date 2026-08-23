package com.example.einkarcade.catalog

import com.example.einkarcade.sokoban.Level

object LevelSummaryMapper {
    fun map(level: Level): LevelSummary =
        LevelSummary(
            puzzleId = level.puzzleId,
            name = level.name,
            isCompleted = level.isCompleted,
            boardGeometry = mapGeometry(level),
        )

    private fun mapGeometry(level: Level): LevelBoardGeometry {
        return LevelBoardGeometry(
            tileMap = level.tileMap,
            player = level.playerStart,
            boxes = level.boxPositions,
        )
    }
}
