package com.example.einkarcade.session

import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position

class GameSession(
    val level: Level,
) {
    private var engine = GameEngine(level)

    val playerPosition: Position
        get() = engine.playerPosition

    val boxPositions: Set<Position>
        get() = engine.boxPositions

    val isAtStart: Boolean
        get() = engine.isAtStart

    val isLevelSolved: Boolean
        get() = engine.isLevelSolved

    val isCleanSolution: Boolean
        get() = engine.isCleanSolution

    val boxMoveHistory: List<List<Position>>
        get() = engine.getBoxMoveHistory()

    fun restart() {
        engine = GameEngine(level)
    }

    fun undoLastMoveAt(position: Position): Boolean = engine.undoLastMoveAt(position) != null

    fun movePlayerTo(position: Position): Boolean = engine.movePlayerTo(position)

    fun moveBox(
        from: Position,
        to: Position,
    ): GameEngine.BoxMoveResult = engine.moveBox(from, to)
}
