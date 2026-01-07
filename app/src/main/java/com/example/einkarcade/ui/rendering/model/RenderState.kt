package com.example.einkarcade.ui.rendering.model

import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.LevelTransition

internal data class RenderState(
    var tiles: List<List<Tile>> = emptyList(),
    var boxPositions: Set<Position> = emptySet(),
    var playerPosition: Position? = null,
    var displayedPlayerPosition: Position? = null,
    var pendingPlayerPosition: Position? = null,
    var selectedBox: Position? = null,
    var isFacingLeft: Boolean = false,
    var pendingFacingLeft: Boolean? = null,
    var isInitialized: Boolean = false
)

internal data class TransitionState(
    var transition: LevelTransition? = null
)
