package com.example.einkarcade.ui.rendering

import com.example.einkarcade.sokoban.Tile

internal data class LevelTransition(
    val oldTiles: List<List<Tile>>,
    val newTiles: List<List<Tile>>,
    val startMs: Long
) {
    val rows: Int = newTiles.size
    val cols: Int = newTiles.firstOrNull()?.size ?: 0

    private val maxIndex: Int = if (rows == 0 || cols == 0) 0 else (rows - 1) + (cols - 1)

    fun isComplete(nowMs: Long): Boolean {
        val total = VanishSpec.totalDurationMs()
        val endMs = startMs + maxIndex * PER_TILE_DELAY_MS + totalDurationMs()
        return nowMs >= endMs
    }

    fun growScale(nowMs: Long, row: Int, col: Int): Float? {
        val local = localElapsedMs(nowMs, row, col)
        if (local < 0) return null
        val total = totalDurationMs()
        if (local >= total) return 1.0f
        val stepCount = GROW_SCALES.size
        if (stepCount <= 1) return 1.0f
        val stepDuration = total.toFloat() / (stepCount - 1).toFloat()
        val stepIndex = (local.toFloat() / stepDuration).toInt().coerceIn(0, stepCount - 2)
        return GROW_SCALES[stepIndex]
    }

    private fun localElapsedMs(nowMs: Long, row: Int, col: Int): Long {
        val index = (rows - 1 - row) + col
        return nowMs - (startMs + index * PER_TILE_DELAY_MS)
    }

    private fun totalDurationMs(): Long {
        return (VanishSpec.totalDurationMs() * TRANSITION_DURATION_MULT).toLong()
    }

    fun tileAt(tiles: List<List<Tile>>, row: Int, col: Int): Tile {
        if (row < 0 || col < 0) return Tile.WALL
        val rowList = tiles.getOrNull(row) ?: return Tile.WALL
        return rowList.getOrNull(col) ?: Tile.WALL
    }

    fun isCellReady(nowMs: Long, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0 || row >= rows || col >= cols) return false
        val local = localElapsedMs(nowMs, row, col)
        return local >= totalDurationMs()
    }

    companion object {
        const val PER_TILE_DELAY_MS: Long = 65L
        const val TRANSITION_DURATION_MULT: Float = 0.08f
        private val GROW_SCALES = floatArrayOf(0.18f, 0.32f, 0.50f, 0.70f, 1.00f)
    }
}
