package com.example.einkarcade.sokoban

class BoxMover(
    private val staticGrid: Array<Array<Boolean>>
) {

    fun canMoveBox(from: Position, to: Position, playerStart: Position): Position? {
        if (from == to) return playerStart
        val numRows = staticGrid.size
        val numCols = staticGrid[0].size

        data class State(val box: Position, val player: Position)

        val visited = mutableSetOf<Pair<Position, Position>>()
        val queue = ArrayDeque<State>()
        queue.add(State(from, playerStart))
        visited.add(from to playerStart)

        val directions = listOf(
            Position(-1, 0), Position(1, 0),
            Position(0, -1), Position(0, 1)
        )

        fun isInside(pos: Position): Boolean {
            return pos.row in 0 until numRows && pos.col in 0 until numCols
        }

        while (queue.isNotEmpty()) {
            val (box, player) = queue.removeFirst()
            if (box == to) return player

            for (dir in directions) {
                val newBox = Position(box.row + dir.row, box.col + dir.col)
                val pushPos = Position(box.row - dir.row, box.col - dir.col)

                if (
                    isInside(newBox) &&
                    isInside(pushPos) &&
                    staticGrid[newBox.row][newBox.col] &&
                    staticGrid[pushPos.row][pushPos.col]
                ) {
                    // Use a fresh grid for each pathfinder instance
                    val pathfinder = pathfinderWithBoxAt(box)
                    if (pathfinder.canFindPath(player, pushPos)) {
                        val newPlayer = box
                        val newState = State(newBox, newPlayer)
                        if ((newBox to newPlayer) !in visited) {
                            visited.add(newBox to newPlayer)
                            queue.add(newState)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun pathfinderWithBoxAt(box: Position): Pathfinder {
        val tempGrid = Array(staticGrid.size) { row ->
            staticGrid[row].copyOf()
        }
        tempGrid[box.row][box.col] = false
        return Pathfinder(tempGrid)
    }
}