package com.example.einkarcade.sokoban

import java.util.PriorityQueue

class Pathfinder(
    private val walkableGrid: Array<Array<Boolean>>
) {

    fun canFindPath(from: Position, to: Position): Boolean {
        if (from == to) return true
        val numRows = walkableGrid.size
        val numCols = walkableGrid[0].size

        val openSet = PriorityQueue(compareBy<Pair<Position, Int>> { it.second })
        val gScore = Array(numRows) { IntArray(numCols) { Int.MAX_VALUE } }
        val visited = Array(numRows) { BooleanArray(numCols) }

        fun heuristic(a: Position, b: Position): Int {
            return kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)
        }

        gScore[from.row][from.col] = 0
        openSet.add(from to heuristic(from, to))

        val directions = listOf(
            Position(-1, 0), // up
            Position(1, 0),  // down
            Position(0, -1), // left
            Position(0, 1)   // right
        )

        while (openSet.isNotEmpty()) {
            val entry = openSet.poll() ?: break
            val (current, _) = entry

            if (current == to) return true
            if (visited[current.row][current.col]) continue
            visited[current.row][current.col] = true

            for (dir in directions) {
                val neighbor = Position(current.row + dir.row, current.col + dir.col)
                if (neighbor.row in 0 until numRows && neighbor.col in 0 until numCols && walkableGrid[neighbor.row][neighbor.col]) {
                    val tentativeG = gScore[current.row][current.col] + 1
                    if (tentativeG < gScore[neighbor.row][neighbor.col]) {
                        gScore[neighbor.row][neighbor.col] = tentativeG
                        val fScore = tentativeG + heuristic(neighbor, to)
                        openSet.add(neighbor to fScore)
                    }
                }
            }
        }

        return false
    }

}

