package com.example.einkarcade.sokoban

import kotlin.math.abs

class GameEngine(private val level: Level) {
    private var gameState = GameState.fromLevel(level)
    private var lastSavedState: GameState? = null

    val playerPosition: Position
        get() = gameState.playerPosition

    val boxPositions: Set<Position>
        get() = gameState.boxPositions

    val isGameWon: Boolean
        get() = gameState.boxPositions.all { level.isGoal(it) }

    val isCleanWin: Boolean
        get() = isGameWon && gameState.boxPositions.size == level.boxPositions.size

    private fun hasBoxAt(position: Position): Boolean {
        return gameState.boxPositions.contains(position)
    }

    fun undo(): Boolean {
        val savedState = lastSavedState ?: return false
        gameState = savedState.deepCopy()
        lastSavedState = null
        return true
    }

    fun moveBoxTo(from: Position, to: Position): List<Position>? {
        if (isGameWon) return null
        if (!hasBoxAt(from)) return null

        val dirRow = from.row - playerPosition.row
        val dirCol = from.col - playerPosition.col
        val isAdjacentPush = abs(dirRow) + abs(dirCol) == 1
        val pushedTo = Position(from.row + dirRow, from.col + dirCol)
        val pushedIntoWall = isAdjacentPush &&
            pushedTo == to &&
            level.grid[to.row][to.col] == Tile.WALL

        if (pushedIntoWall) {
            lastSavedState = gameState.deepCopy()
            gameState.removeBox(from)
            gameState.movePlayer(from)
            return listOf(from, to)
        }

        // Plan a multi-push move using BoxMover. The walkable grid treats boxes as obstacles,
        // so mark the starting box square walkable for the planning step.
        val gridCopy = walkableGrid.map { it.copyOf() }.toTypedArray()
        gridCopy[from.row][from.col] = true

        val boxMover = BoxMover(gridCopy)
        val boxPath = boxMover.canMoveBox(from, to, playerPosition) ?: return null
        val finalPlayerPosition = if (boxPath.size >= 2) {
            boxPath[boxPath.size - 2]
        } else {
            boxPath.last()
        }

        // Apply the planned move.
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(from, to)
        gameState.movePlayer(finalPlayerPosition)
        return boxPath
    }

    fun movePlayerTo(position: Position): Boolean {
        if (isGameWon) return false

        val pathfinder = Pathfinder(walkableGrid)
        if (!pathfinder.canFindPath(playerPosition, position)) return false
        if (position == playerPosition) return false

        gameState.movePlayer(position)
        return true
    }

    private val walkableGrid: Array<Array<Boolean>>
        get() = Array(level.grid.size) { row ->
            Array(level.grid[0].size) { col ->
                val pos = Position(row, col)
                level.grid[row][col] != Tile.WALL && !gameState.boxPositions.contains(pos)
            }
        }
}
