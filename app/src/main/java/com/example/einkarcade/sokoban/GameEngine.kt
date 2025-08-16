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

    fun move(direction: Direction) {
        if (isGameWon) return

        val targetPosition = playerPosition.move(direction)
        if (level.isWall(targetPosition)) return

        if (hasBoxAt(targetPosition)) {
            if (canPushBox(targetPosition, direction)) {
                pushBox(targetPosition, direction)
            }
        } else {
            gameState.movePlayer(targetPosition)
        }
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

    fun undo() {
        val savedState = lastSavedState ?: return
        gameState = savedState.deepCopy()
        lastSavedState = null
    }

    fun moveBoxTo(from: Position, to: Position, playerEnd: Position) {
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(from, to)
        gameState.movePlayer(playerEnd)
    }

    fun moveTo(position: Position) {
        if (hasBoxAt(position) && isAdjacent(playerPosition, position)) {
            val direction = directionTo(playerPosition, position)
            if (direction != null) {
                move(direction)
            }
        } else {
            val pathfinder = Pathfinder(walkableGrid)
            if (pathfinder.canFindPath(playerPosition, position)) {
                gameState.movePlayer(position)
            }
        }
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

    val walkableGrid: Array<Array<Boolean>>
        get() = Array(level.grid.size) { row ->
            Array(level.grid[0].size) { col ->
                val pos = Position(row, col)
                level.grid[row][col] != Tile.WALL && !gameState.boxPositions.contains(pos)
            }
        }
}