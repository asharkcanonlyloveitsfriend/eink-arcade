package com.example.einkarcade.data

import java.util.ArrayDeque

object LevelNormalizer {
    fun normalize(level: String): String {
        require(level.any { tile -> tile == '@' || tile == '+' }) { "Level does not contain a player." }

        val rows = level.lines()
        val width = rows.maxOf(String::length)
        val grid =
            rows.map { row -> row.padEnd(width, '#').toCharArray() }

        maskUnreachableTiles(grid)
        return trimOuterWalls(grid)
    }

    private fun maskUnreachableTiles(grid: List<CharArray>) {
        val player =
            grid.indices
                .flatMap { row -> grid[row].indices.map { column -> row to column } }
                .first { (row, column) -> grid[row][column] == '@' || grid[row][column] == '+' }
        val visited = Array(grid.size) { BooleanArray(grid.first().size) }
        val pending = ArrayDeque<Pair<Int, Int>>()
        pending.add(player)
        visited[player.first][player.second] = true

        while (pending.isNotEmpty()) {
            val (row, column) = pending.removeLast()
            for (
            (neighborRow, neighborColumn) in
            arrayOf(row - 1 to column, row + 1 to column, row to column - 1, row to column + 1)
            ) {
                if (
                    neighborRow in grid.indices &&
                    neighborColumn in grid.first().indices &&
                    !visited[neighborRow][neighborColumn] &&
                    grid[neighborRow][neighborColumn] != '#'
                ) {
                    visited[neighborRow][neighborColumn] = true
                    pending.add(neighborRow to neighborColumn)
                }
            }
        }

        grid.forEachIndexed { row, tiles ->
            tiles.indices.filter { column -> !visited[row][column] }.forEach { column ->
                tiles[column] = '#'
            }
        }
    }

    private fun trimOuterWalls(grid: List<CharArray>): String {
        var top = 0
        var bottom = grid.lastIndex
        var left = 0
        var right = grid.first().lastIndex

        while (top < bottom && grid[top].all { it == '#' }) top++
        while (bottom > top && grid[bottom].all { it == '#' }) bottom--
        while (left < right && (top..bottom).all { row -> grid[row][left] == '#' }) left++
        while (right > left && (top..bottom).all { row -> grid[row][right] == '#' }) right--

        return (top..bottom).joinToString("\n") { row -> grid[row].concatToString(left, right + 1) }
    }
}
