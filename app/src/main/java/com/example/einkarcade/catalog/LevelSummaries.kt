package com.example.einkarcade.catalog

import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap

data class LevelSetSummary(
    val id: Int,
    val name: String,
    val levelCount: Int,
    val completedCount: Int,
)

data class LevelSummary(
    val puzzleId: Int,
    val name: String,
    val isCompleted: Boolean,
    val boardGeometry: LevelBoardGeometry,
)

data class LevelBoardGeometry(
    val tileMap: TileMap,
    val player: Position,
    val boxes: Set<Position>,
)
