package com.example.einkarcade.sokoban

data class Position(val row: Int, val col: Int) {
    fun move(direction: Direction): Position = when (direction) {
        Direction.UP -> Position(row - 1, col)
        Direction.DOWN -> Position(row + 1, col)
        Direction.LEFT -> Position(row, col - 1)
        Direction.RIGHT -> Position(row, col + 1)
    }
}