package com.example.einkarcade.ui.rendering

import com.example.einkarcade.sokoban.Tile

internal data class LevelTransition(
    val oldTiles: List<List<Tile>>,
    val newTiles: List<List<Tile>>,
    val startTick: Long
) {
    val rows: Int = newTiles.size
    val cols: Int = newTiles.firstOrNull()?.size ?: 0

    private val maxIndex: Int = if (rows == 0 || cols == 0) 0 else (rows - 1) + (cols - 1)

    fun isComplete(nowMs: Long): Boolean {
        val nowTick = RenderTimings.nowTick(nowMs)
        val endTick = startTick + maxIndex * PER_TILE_DELAY_TICKS + totalDurationTicks()
        return nowTick >= endTick
    }

    fun growScale(nowMs: Long, row: Int, col: Int): Float? {
        val local = localElapsedTicks(nowMs, row, col)
        if (local < 0) return null
        val total = totalDurationTicks()
        if (local >= total) return 1.0f
        val stepCount = GROW_SCALES.size
        if (stepCount <= 1) return 1.0f
        val stepDuration = total.toFloat() / (stepCount - 1).toFloat()
        val stepIndex = (local.toFloat() / stepDuration).toInt().coerceIn(0, stepCount - 2)
        return GROW_SCALES[stepIndex]
    }

    private fun localElapsedTicks(nowMs: Long, row: Int, col: Int): Long {
        val nowTick = RenderTimings.nowTick(nowMs)
        val index = (rows - 1 - row) + col
        return nowTick - (startTick + index * PER_TILE_DELAY_TICKS)
    }

    private fun totalDurationTicks(): Long {
        return kotlin.math.ceil(
            VanishSpec.totalDurationTicks().toDouble() * TRANSITION_DURATION_MULT
        ).toLong()
    }

    fun tileAt(tiles: List<List<Tile>>, row: Int, col: Int): Tile {
        if (row < 0 || col < 0) return Tile.WALL
        val rowList = tiles.getOrNull(row) ?: return Tile.WALL
        return rowList.getOrNull(col) ?: Tile.WALL
    }

    fun isCellReady(nowMs: Long, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0 || row >= rows || col >= cols) return false
        val local = localElapsedTicks(nowMs, row, col)
        return local >= totalDurationTicks()
    }

    companion object {
        const val TRANSITION_DURATION_MULT: Float = 0.08f
        const val PER_TILE_DELAY_TICKS: Long = 2L
        private val GROW_SCALES = floatArrayOf(0.18f, 0.32f, 0.50f, 0.70f, 1.00f)
    }
}
