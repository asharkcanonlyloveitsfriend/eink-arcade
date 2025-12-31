package com.example.einkarcade.sokoban

data class Level(
    val name: String,
    val ascii: String,
    val grid: List<List<Tile>>,
    val playerStart: Position,
    val boxPositions: Set<Position>,
    val puzzleId: Int = -1
)
{
    // -1 = thumbs down, 0 = none, 1 = thumbs up. Not part of equality/hashCode.
    var rating: Int = 0
        private set
    var completedAt: String? = null
        private set

    val isCompleted: Boolean
        get() = completedAt != null


    fun setRating(value: Int) {
        rating = value
    }


    fun markCompleted(timestamp: String) {
        completedAt = timestamp
    }

    fun setCompletedAt(value: String?) {
        completedAt = value
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
        fun fromAscii(name: String, ascii: String, puzzleId: Int = -1): Level {
            val parsed = parseAscii(ascii)
            val exterior = findExteriorWalkableCells(parsed.initialGrid)
            val finalGrid = applyExteriorMask(parsed.initialGrid, exterior)
            return Level(name, ascii, finalGrid, parsed.playerStart, parsed.boxes, puzzleId)
        }

        private data class ParsedAscii(
            val initialGrid: List<List<Tile>>,
            val playerStart: Position,
            val boxes: Set<Position>
        )

        /** Parses Sokoban ASCII into a base grid (WALL/FLOOR/GOAL) and extracts player + boxes. */
        private fun parseAscii(ascii: String): ParsedAscii {
            val lines = ascii.lines().dropLastWhile { it.isBlank() }
            val maxWidth = lines.maxOfOrNull { it.length } ?: 0

            var playerStart: Position? = null
            val boxes = mutableSetOf<Position>()

            val grid = lines.mapIndexed { rowIndex, line ->
                line.padEnd(maxWidth).mapIndexed { colIndex, ch ->
                    val position = Position(rowIndex, colIndex)
                    when (ch) {
                        '#' -> Tile.WALL
                        '.' -> Tile.GOAL
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
                        else -> Tile.FLOOR
                    }
                }
            }

            val start = requireNotNull(playerStart) { "Player start '@' not found in level" }
            return ParsedAscii(grid, start, boxes)
        }

        /** Marks exterior walkable cells by flood-filling from the boundary across floor tiles. */
        private fun findExteriorWalkableCells(grid: List<List<Tile>>): Array<BooleanArray> {
            val numRows = grid.size
            val numCols = grid.firstOrNull()?.size ?: 0
            val visited = Array(numRows) { BooleanArray(numCols) }
            if (numRows == 0 || numCols == 0) return visited

            val queue = ArrayDeque<Int>()

            fun enqueue(row: Int, col: Int) {
                if (!visited[row][col] && grid[row][col] == Tile.FLOOR) {
                    visited[row][col] = true
                    queue.add(row * numCols + col)
                }
            }

            // Seed BFS from all boundary walkable cells.
            for (col in 0 until numCols) {
                enqueue(0, col)
                if (numRows > 1) enqueue(numRows - 1, col)
            }
            for (row in 1 until numRows - 1) {
                enqueue(row, 0)
                if (numCols > 1) enqueue(row, numCols - 1)
            }

            val dr = intArrayOf(-1, 1, 0, 0)
            val dc = intArrayOf(0, 0, -1, 1)

            while (queue.isNotEmpty()) {
                val packed = queue.removeFirst()
                val row = packed / numCols
                val col = packed % numCols

                for (i in 0..3) {
                    val nr = row + dr[i]
                    val nc = col + dc[i]
                    if (nr in 0 until numRows && nc in 0 until numCols) {
                        enqueue(nr, nc)
                    }
                }
            }

            return visited
        }

        private fun applyExteriorMask(
            grid: List<List<Tile>>,
            exterior: Array<BooleanArray>
        ): List<List<Tile>> {
            val numRows = grid.size
            val numCols = grid.firstOrNull()?.size ?: 0
            return List(numRows) { row ->
                List(numCols) { col ->
                    if (exterior[row][col]) Tile.EMPTY else grid[row][col]
                }
            }
        }
    }

    fun isGoal(position: Position): Boolean {
        return grid.getOrNull(position.row)?.getOrNull(position.col) == Tile.GOAL
    }
}
