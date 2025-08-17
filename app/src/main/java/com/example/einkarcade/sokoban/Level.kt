package com.example.einkarcade.sokoban

data class Level(
    val name: String,
    val ascii: String,
    val grid: List<List<Tile>>,
    val playerStart: Position,
    val boxPositions: Set<Position>
)
{
    // -1 = thumbs down, 0 = none, 1 = thumbs up. Not part of equality/hashCode.
    var rating: Int = 0
        private set
    var completedAt: Long = 0L
        private set

    val isCompleted: Boolean
        get() = completedAt > 0L


    fun setRating(value: Int) {
        rating = value
    }

    fun setCompletedAt(value: Long) {
        completedAt = value
    }

    fun markCompleted() {
        completedAt = System.currentTimeMillis()
    }

    fun toggleThumbUp(): Int {
        rating = if (rating == 1) 0 else 1
        return rating
    }

    fun toggleThumbDown(): Int {
        rating = if (rating == -1) 0 else -1
        return rating
    }


    companion object {
        fun fromAscii(name: String, ascii: String): Level {
            val lines = ascii.lines().dropLastWhile { it.isBlank() }
            val maxWidth = lines.maxOfOrNull { it.length } ?: 0
            var playerStart: Position? = null
            val boxes = mutableSetOf<Position>()
            val grid = lines.mapIndexed { rowIndex, line ->
                line.padEnd(maxWidth).mapIndexed { colIndex, char ->
                    val position = Position(rowIndex, colIndex)
                    when (char) {
                        '#' -> Tile.WALL
                        '.' -> Tile.TARGET
                        '$' -> {
                            boxes.add(position)
                            Tile.EMPTY
                        }
                        '*' -> {
                            boxes.add(position)
                            Tile.TARGET
                        }
                        '@' -> {
                            playerStart = position
                            Tile.EMPTY
                        }
                        '+' -> {
                            playerStart = position
                            Tile.TARGET
                        }
                        else -> Tile.EMPTY
                    }
                }
            }
            requireNotNull(playerStart) { "Player start '@' not found in level" }
            return Level(name, ascii, grid, playerStart!!, boxes)
        }
    }

    fun isWall(position: Position): Boolean {
        return grid.getOrNull(position.row)?.getOrNull(position.col) == Tile.WALL
    }

    fun isTarget(position: Position): Boolean {
        return grid.getOrNull(position.row)?.getOrNull(position.col) == Tile.TARGET
    }

    fun isPassable(position: Position): Boolean {
        return isInBounds(position) && !isWall(position)
    }

    private fun isInBounds(position: Position): Boolean {
        return position.row in grid.indices && position.col in grid[0].indices
    }
}