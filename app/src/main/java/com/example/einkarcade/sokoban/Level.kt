package com.example.einkarcade.sokoban

data class Level(
    val name: String,
    val grid: List<List<Tile>>,
    val playerStart: Position,
    val boxPositions: Set<Position>
) {
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
            return Level(name, grid, playerStart!!, boxes)
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