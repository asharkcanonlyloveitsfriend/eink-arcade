package com.example.einkarcade.ui.rendering

import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile

internal data class LevelInit(
    val tiles: List<List<Tile>>,
    val playerPosition: Position,
    val boxPositions: Set<Position>
)
