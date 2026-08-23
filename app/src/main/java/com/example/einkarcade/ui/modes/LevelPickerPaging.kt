package com.example.einkarcade.ui.modes

import kotlin.math.min

internal object LevelPickerPaging {
    const val GRID_SIZE = 3
    const val PAGE_SIZE = GRID_SIZE * GRID_SIZE

    fun lastPageStart(levelCount: Int): Int = (levelCount - PAGE_SIZE).coerceAtLeast(0)

    fun initialPageStart(
        selectedIndex: Int,
        levelCount: Int,
    ): Int {
        if (selectedIndex !in 0 until levelCount) return 0

        val selectedRow = selectedIndex / GRID_SIZE
        val centeredStartRow = (selectedRow - (GRID_SIZE / 2)).coerceAtLeast(0)
        return min(centeredStartRow * GRID_SIZE, lastPageStart(levelCount))
    }

    fun selectedRowScrollFraction(
        selectedIndex: Int,
        levelCount: Int,
    ): Float {
        if (selectedIndex !in 0 until levelCount) return 0f

        val selectedRow = selectedIndex / GRID_SIZE
        val lastRow = (levelCount - 1) / GRID_SIZE
        return if (lastRow > 0) selectedRow.toFloat() / lastRow else 0f
    }

    fun previousPageStart(currentStart: Int): Int =
        (currentStart - PAGE_SIZE).coerceAtLeast(0)

    fun nextPageStart(
        currentStart: Int,
        levelCount: Int,
    ): Int = min(currentStart + PAGE_SIZE, lastPageStart(levelCount))
}
