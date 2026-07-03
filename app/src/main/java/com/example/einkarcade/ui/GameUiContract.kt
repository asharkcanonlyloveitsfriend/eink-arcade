package com.example.einkarcade.ui

import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap

data class GameScreenState(
    val setName: String,
    val setId: Int,
    val levelName: String,
    val puzzleId: Int,
    val rating: Int,
    val isStarred: Boolean,
    val tileMap: TileMap,
)

data class LevelTransitionSnapshot(
    val oldTileMap: TileMap,
)

enum class GameUiMode {
    GAMEPLAY,
    LEVEL_SOLVED,
    LEVEL_TRANSITION,
}

sealed interface GameRenderEvent {
    data class StateChanged(
        val playerPosition: Position,
        val boxPositions: Set<Position>,
        val annotation: StateChangeAnnotation? = null,
    ) : GameRenderEvent

    sealed interface StateChangeAnnotation {
        data class BoxRemoved(val position: Position) : StateChangeAnnotation

        data class BoxMoved(val path: List<Position>) : StateChangeAnnotation
    }

    data object LevelSolvedWithCheat : GameRenderEvent

    data object MoveRejected : GameRenderEvent
}
