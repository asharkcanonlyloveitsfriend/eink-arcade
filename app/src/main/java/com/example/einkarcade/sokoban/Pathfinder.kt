package com.example.einkarcade.sokoban

class Pathfinder(
    private val walkableGrid: Array<Array<Boolean>>,
    private val stats: PathfinderStats? = null,
) {
    fun canFindPath(
        from: Position,
        to: Position,
    ): Boolean {
        if (from == to) return true
        val numRows = walkableGrid.size
        val numCols = walkableGrid[0].size

        val visited = Array(numRows) { BooleanArray(numCols) }
        val queue: ArrayDeque<Int> = ArrayDeque()

        queue.add(from.row * numCols + from.col)
        stats?.nodesPushed = stats?.nodesPushed?.plus(1) ?: 0

        val targetIndex = to.row * numCols + to.col

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            stats?.nodesExpanded = stats?.nodesExpanded?.plus(1) ?: 0

            if (current == targetIndex) return true

            val row = current / numCols
            val col = current % numCols

            if (visited[row][col]) continue
            visited[row][col] = true

            // up
            val up = row - 1
            if (up >= 0 && walkableGrid[up][col] && !visited[up][col]) {
                queue.add(up * numCols + col)
                stats?.nodesPushed = stats?.nodesPushed?.plus(1) ?: 0
            }

            // down
            val down = row + 1
            if (down < numRows && walkableGrid[down][col] && !visited[down][col]) {
                queue.add(down * numCols + col)
                stats?.nodesPushed = stats?.nodesPushed?.plus(1) ?: 0
            }

            // left
            val left = col - 1
            if (left >= 0 && walkableGrid[row][left] && !visited[row][left]) {
                queue.add(row * numCols + left)
                stats?.nodesPushed = stats?.nodesPushed?.plus(1) ?: 0
            }

            // right
            val right = col + 1
            if (right < numCols && walkableGrid[row][right] && !visited[row][right]) {
                queue.add(row * numCols + right)
                stats?.nodesPushed = stats?.nodesPushed?.plus(1) ?: 0
            }
        }

        return false
    }
}
