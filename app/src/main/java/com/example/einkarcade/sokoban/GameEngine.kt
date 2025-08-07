package com.example.einkarcade.sokoban

class GameEngine(private val level: Level) {
    private var gameState = GameState.fromLevel(level)
    private var lastSavedState: GameState? = null

    val playerPosition: Position
        get() = gameState.playerPosition

    val boxPositions: Set<Position>
        get() = gameState.boxPositions

    val isGameWon: Boolean
        get() = gameState.boxPositions.all { level.isTarget(it) }

    fun step(direction: Direction): Boolean {
        if (isGameWon) return false

        val targetPosition = playerPosition.move(direction)
        if (level.isWall(targetPosition)) return false

        if (hasBoxAt(targetPosition)) {
            if (!canPushBox(targetPosition, direction)) return false
            pushBox(targetPosition, direction)
            return true
        }

        gameState.movePlayer(targetPosition)
        return true
    }

    private fun canPushBox(boxPosition: Position, direction: Direction): Boolean {
        val newBoxPosition = boxPosition.move(direction)
        return level.isPassable(newBoxPosition) && !hasBoxAt(newBoxPosition)
    }

    private fun pushBox(boxPosition: Position, direction: Direction) {
        val newBoxPosition = boxPosition.move(direction)
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(boxPosition, newBoxPosition)
        gameState.movePlayer(boxPosition)
    }

    private fun hasBoxAt(position: Position): Boolean {
        return gameState.boxPositions.contains(position)
    }

    fun undo(): Boolean {
        val savedState = lastSavedState ?: return false
        gameState = savedState.deepCopy()
        lastSavedState = null
        return true
    }

    fun moveBoxTo(from: Position, to: Position): Boolean {
        if (isGameWon) return false
        if (!hasBoxAt(from)) return false

        // Plan a multi-push move using BoxMover. The walkable grid treats boxes as obstacles,
        // so mark the starting box square walkable for the planning step.
        val gridCopy = walkableGrid.map { it.copyOf() }.toTypedArray()
        gridCopy[from.row][from.col] = true

        val boxMover = BoxMover(gridCopy)
        val finalPlayerPosition = boxMover.canMoveBox(from, to, playerPosition) ?: return false

        // Apply the planned move.
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(from, to)
        gameState.movePlayer(finalPlayerPosition)
        return true
    }

    fun movePlayerTo(position: Position): Boolean {
        if (isGameWon) return false

        if (hasBoxAt(position) && isAdjacent(playerPosition, position)) {
            val direction = directionTo(playerPosition, position) ?: return false
            return step(direction)
        }

        val pathfinder = Pathfinder(walkableGrid)
        if (!pathfinder.canFindPath(playerPosition, position)) return false
        if (position == playerPosition) return false

        gameState.movePlayer(position)
        return true
    }

    private fun isAdjacent(a: Position, b: Position): Boolean {
        val rowDiff = kotlin.math.abs(a.row - b.row)
        val colDiff = kotlin.math.abs(a.col - b.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    private fun directionTo(from: Position, to: Position): Direction? {
        return when {
            from.row == to.row && from.col + 1 == to.col -> Direction.RIGHT
            from.row == to.row && from.col - 1 == to.col -> Direction.LEFT
            from.row + 1 == to.row && from.col == to.col -> Direction.DOWN
            from.row - 1 == to.row && from.col == to.col -> Direction.UP
            else -> null
        }
    }

    private val walkableGrid: Array<Array<Boolean>>
        get() = Array(level.grid.size) { row ->
            Array(level.grid[0].size) { col ->
                val pos = Position(row, col)
                level.grid[row][col] != Tile.WALL && !gameState.boxPositions.contains(pos)
            }
        }
}