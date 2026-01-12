package com.example.einkarcade.ui.rendering.model

import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.LevelTransition

internal data class RenderStateStateful(
    var tiles: List<List<Tile>> = emptyList(),
    var boxPositions: Set<Position> = emptySet(),
    var playerPosition: Position? = null,
    var displayedPlayerPosition: Position? = null,
    var pendingPlayerPosition: Position? = null,
    var selectedBox: Position? = null,
    var isFacingLeft: Boolean = false,
    var pendingFacingLeft: Boolean? = null
) {
    val isReady: Boolean
        get() = tiles.isNotEmpty() && playerPosition != null
}

internal data class TransitionState(
    var transition: LevelTransition? = null
)
