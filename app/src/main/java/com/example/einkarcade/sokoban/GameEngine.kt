package com.example.einkarcade.sokoban

import kotlin.math.abs

class GameEngine(
    private val level: Level,
) {
    private var gameState = GameState.fromLevel(level)
    private val boxMoveHistory: MutableList<List<Position>> = mutableListOf()

    val playerPosition: Position
        get() = gameState.playerPosition

    val boxPositions: Set<Position>
        get() = gameState.boxPositions

    val boxMoveCount: Int
        get() = boxMoveHistory.size

    val isLevelSolved: Boolean
        get() = gameState.boxPositions.all { level.tileMap.isGoal(it) }

    val isCleanSolution: Boolean
        get() = isLevelSolved && gameState.boxPositions.size == level.boxPositions.size

    val isAtStart: Boolean
        get() =
            gameState.playerPosition == level.playerStart &&
                boxMoveHistory.isEmpty()

    fun getBoxMoveHistory(): List<List<Position>> = boxMoveHistory.toList()

    fun undoLastMoveAt(position: Position): List<Position>? {
        if (isLevelSolved) return null
        if (boxMoveHistory.lastOrNull()?.last() != position) return null
        return undoLastMove()
    }

    private fun undoLastMove(): List<Position>? {
        val path = boxMoveHistory.removeLastOrNull() ?: return null

        val boxFrom = path.first()
        val boxTo = path.last()
        val firstStep =
            Position(
                row = path[1].row - boxFrom.row,
                col = path[1].col - boxFrom.col,
            )
        val newPlayerPosition =
            Position(
                row = boxFrom.row - firstStep.row,
                col = boxFrom.col - firstStep.col,
            )

        gameState.removeBox(boxTo)
        gameState.addBox(boxFrom)
        gameState.movePlayer(newPlayerPosition)
        return path
    }

    fun moveBox(
        from: Position,
        to: Position,
    ): BoxMoveResult {
        if (isLevelSolved) return BoxMoveResult.Rejected
        if (!gameState.hasBoxAt(from)) return BoxMoveResult.Rejected

        return if (level.tileMap.isVoid(to)) {
            pushBoxIntoVoid(from, to)
        } else {
            performBoxMove(from, to)
        }
    }

    private fun performBoxMove(
        from: Position,
        to: Position,
    ): BoxMoveResult {
        val boxPathfinder =
            BoxPathfinder(
                fullGrid = walkableGrid,
                boxStart = from,
                playerStart = playerPosition,
            )

        val boxPath = boxPathfinder.findBoxPath(to) ?: return BoxMoveResult.Rejected
        val finalPlayerPosition =
            if (boxPath.size >= 2) {
                boxPath[boxPath.size - 2]
            } else {
                boxPath.last()
            }

        // Apply the planned move.
        addBoxMoveToHistory(boxPath)
        gameState.moveBox(from, to)
        gameState.movePlayer(finalPlayerPosition)
        return BoxMoveResult.Moved(boxPath)
    }

    private fun pushBoxIntoVoid(
        from: Position,
        to: Position,
    ): BoxMoveResult {
        val dirRow = from.row - playerPosition.row
        val dirCol = from.col - playerPosition.col
        val isAdjacentPush = abs(dirRow) + abs(dirCol) == 1
        val pushedTo = Position(from.row + dirRow, from.col + dirCol)

        if (!isAdjacentPush) return BoxMoveResult.Rejected
        if (pushedTo != to) return BoxMoveResult.Rejected
        if (!level.tileMap.isVoid(to)) return BoxMoveResult.Rejected

        addBoxMoveToHistory(listOf(from, to))
        gameState.removeBox(from)
        gameState.movePlayer(from)
        return BoxMoveResult.Removed(to)
    }

    sealed interface BoxMoveResult {
        data class Moved(
            val path: List<Position>,
        ) : BoxMoveResult

        data class Removed(
            val position: Position,
        ) : BoxMoveResult

        data object Rejected : BoxMoveResult
    }

    fun movePlayerTo(position: Position): Boolean {
        val pathfinder = Pathfinder(walkableGrid)
        if (!pathfinder.canFindPath(playerPosition, position)) return false
        if (position == playerPosition) return false

        gameState.movePlayer(position)
        return true
    }

    private fun addBoxMoveToHistory(move: List<Position>) {
        val previousMove = boxMoveHistory.lastOrNull()
        if (previousMove?.last() == move.first()) {
            boxMoveHistory[boxMoveHistory.lastIndex] = previousMove + move.drop(1)
        } else {
            boxMoveHistory.add(move)
        }
    }

    private val walkableGrid: Array<Array<Boolean>>
        get() =
            WalkableGrid.withObstacles(
                baseGrid = level.tileMap.walkableGrid(),
                obstacles = gameState.boxPositions,
            )
}
