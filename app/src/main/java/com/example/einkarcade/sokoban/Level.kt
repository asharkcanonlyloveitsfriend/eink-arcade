package com.example.einkarcade.sokoban

data class Level(
    val name: String,
    val ascii: String,
    val grid: List<List<Tile>>,
    val playerStart: Position,
    val boxPositions: Set<Position>,
    val puzzleId: Int = -1,
) {
    var completedAt: String? = null
        private set

    val isCompleted: Boolean
        get() = completedAt != null

    val tileMap: TileMap
        get() = TileMap(grid)

    fun markCompleted(timestamp: String) {
        completedAt = timestamp
    }

    fun setCompletedAt(value: String?) {
        completedAt = value
    }

    companion object {
        fun fromAscii(
            name: String,
            ascii: String,
            puzzleId: Int = -1,
        ): Level {
            val parsed = parseAscii(ascii)
            return Level(
                name,
                ascii,
                parsed.initialGrid,
                parsed.playerStart,
                parsed.boxes,
                puzzleId,
            )
        }

        private data class ParsedAscii(
            val initialGrid: List<List<Tile>>,
            val playerStart: Position,
            val boxes: Set<Position>,
        )

        /** Parses Sokoban ASCII into a base grid (VOID/FLOOR/GOAL) and extracts player + boxes. */
        private fun parseAscii(ascii: String): ParsedAscii {
            val lines = ascii.trimEnd('\n', '\r').lines()
            val width = lines.firstOrNull()?.length ?: 0
            require(lines.all { it.length == width }) { "Level rows must all have the same width" }

            var playerStart: Position? = null
            val boxes = mutableSetOf<Position>()

            val grid =
                lines.mapIndexed { rowIndex, line ->
                    line.mapIndexed { colIndex, ch ->
                        val position = Position(rowIndex, colIndex)
                        when (ch) {
                            '#' -> {
                                Tile.VOID
                            }

                            ' ' -> {
                                Tile.FLOOR
                            }

                            '.' -> {
                                Tile.GOAL
                            }

                            '$' -> {
                                boxes.add(position)
                                Tile.FLOOR
                            }

                            '*' -> {
                                boxes.add(position)
                                Tile.GOAL
                            }

                            '@' -> {
                                playerStart = position
                                Tile.FLOOR
                            }

                            '+' -> {
                                playerStart = position
                                Tile.GOAL
                            }

                            else -> {
                                Tile.FLOOR
                            }
                        }
                    }
                }

            val start = requireNotNull(playerStart) { "Player start '@' not found in level" }
            return ParsedAscii(grid, start, boxes)
        }
    }
}
