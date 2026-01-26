package com.example.einkarcade.sokoban

class BoxPathfinder(
    fullGrid: Array<Array<Boolean>>,
    boxStart: Position,
    playerStart: Position
) {
    private data class State(val box: Position, val player: Position)

    private val planningGrid: Array<Array<Boolean>> =
        Array(fullGrid.size) { row ->
            fullGrid[row].copyOf()
        }.also {
            // While planning, the box being moved is treated as walkable
            it[boxStart.row][boxStart.col] = true
        }

    private val startState = State(boxStart, playerStart)

    fun findBoxPath(to: Position): List<Position>? {
        if (startState.box == to) return null
        val numRows = planningGrid.size
        val numCols = planningGrid[0].size

        val visited = mutableSetOf<Pair<Position, Position>>()
        val parents = mutableMapOf<State, State?>()
        val queue = ArrayDeque<State>()
        queue.add(startState)
        visited.add(startState.box to startState.player)
        parents[startState] = null

        val directions = listOf(
            Position(-1, 0), Position(1, 0),
            Position(0, -1), Position(0, 1)
        )

        fun isInside(pos: Position): Boolean {
            return pos.row in 0 until numRows && pos.col in 0 until numCols
        }

        while (queue.isNotEmpty()) {
            val (box, player) = queue.removeFirst()
            if (box == to) {
                return buildBoxPath(parents, State(box, player))
            }

            for (dir in directions) {
                val newBox = Position(box.row + dir.row, box.col + dir.col)
                val pushPos = Position(box.row - dir.row, box.col - dir.col)

                if (
                    isInside(newBox) &&
                    isInside(pushPos) &&
                    planningGrid[newBox.row][newBox.col] &&
                    planningGrid[pushPos.row][pushPos.col]
                ) {
                    val pathfinder = pathfinderWithBoxAt(box)
                    if (pathfinder.canFindPath(player, pushPos)) {
                        val newPlayer = box
                        val newState = State(newBox, newPlayer)
                        if ((newBox to newPlayer) !in visited) {
                            visited.add(newBox to newPlayer)
                            parents[newState] = State(box, player)
                            queue.add(newState)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun buildBoxPath(parents: Map<State, State?>, endState: State): List<Position> {
        val reversed = mutableListOf<Position>()
        var current: State? = endState
        while (current != null) {
            reversed.add(current.box)
            current = parents[current]
        }
        reversed.reverse()
        return reversed
    }

    private fun pathfinderWithBoxAt(box: Position): Pathfinder {
        val tempGrid = Array(planningGrid.size) { row ->
            planningGrid[row].copyOf()
        }
        // For player reachability checks, the box occupies its current square and must be solid.
        tempGrid[box.row][box.col] = false
        return Pathfinder(tempGrid)
    }
}
