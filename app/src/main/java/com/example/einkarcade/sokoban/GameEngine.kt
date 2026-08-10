package com.example.einkarcade.sokoban

import kotlin.math.abs

private const val MAX_UNDOS_ALLOWED = 3

class GameEngine(
    private val level: Level,
    private val maxUndosAllowed: Int = MAX_UNDOS_ALLOWED,
) {
    init {
        require(maxUndosAllowed >= 0) { "maxUndosAllowed must be non-negative" }
    }

    private var gameState = GameState.fromLevel(level)
    private val boxMoveHistory: MutableList<List<Position>> = mutableListOf()
    private var undosAvailable: Int = 0

    val playerPosition: Position
        get() = gameState.playerPosition

    val boxPositions: Set<Position>
        get() = gameState.boxPositions

    val isLevelSolved: Boolean
        get() = gameState.boxPositions.all { level.tileMap.isGoal(it) }

    val isCleanSolution: Boolean
        get() = isLevelSolved && gameState.boxPositions.size == level.boxPositions.size

    val isAtStart: Boolean
        get() =
            gameState.playerPosition == level.playerStart &&
                boxMoveHistory.isEmpty()

    fun getBoxMoveHistory(): List<List<Position>> = boxMoveHistory.toList()

    fun undo(): List<Position>? {
        if (undosAvailable <= 0) return null
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
        undosAvailable--
        return path
    }

    fun moveBox(
        from: Position,
        to: Position,
    ): BoxMoveResult =
        if (level.tileMap.isVoid(to)) {
            pushBoxIntoVoid(from, to)
        } else {
            performBoxMove(from, to)
        }

    private fun performBoxMove(
        from: Position,
        to: Position,
    ): BoxMoveResult {
        if (isLevelSolved) return BoxMoveResult.Rejected
        if (!gameState.hasBoxAt(from)) return BoxMoveResult.Rejected

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
        boxMoveHistory.add(boxPath)
        addUndoCredit()
        gameState.moveBox(from, to)
        gameState.movePlayer(finalPlayerPosition)
        return BoxMoveResult.Moved(boxPath)
    }

    private fun pushBoxIntoVoid(
        from: Position,
        to: Position,
    ): BoxMoveResult {
        if (isLevelSolved) return BoxMoveResult.Rejected
        if (!gameState.hasBoxAt(from)) return BoxMoveResult.Rejected

        val dirRow = from.row - playerPosition.row
        val dirCol = from.col - playerPosition.col
        val isAdjacentPush = abs(dirRow) + abs(dirCol) == 1
        val pushedTo = Position(from.row + dirRow, from.col + dirCol)

        if (!isAdjacentPush) return BoxMoveResult.Rejected
        if (pushedTo != to) return BoxMoveResult.Rejected
        if (!level.tileMap.isVoid(to)) return BoxMoveResult.Rejected

        boxMoveHistory.add(listOf(from, to))
        addUndoCredit()
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
        if (isLevelSolved) return false

        val pathfinder = Pathfinder(walkableGrid)
        if (!pathfinder.canFindPath(playerPosition, position)) return false
        if (position == playerPosition) return false

        gameState.movePlayer(position)
        return true
    }

    private fun addUndoCredit() {
        undosAvailable = minOf(undosAvailable + 1, maxUndosAllowed)
    }

    private val walkableGrid: Array<Array<Boolean>>
        get() =
            WalkableGrid.withObstacles(
                baseGrid = level.tileMap.walkableGrid(),
                obstacles = gameState.boxPositions,
            )
}
