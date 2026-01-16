package com.example.einkarcade.ui.rendering.anim

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.VanishSpec
import com.example.einkarcade.ui.rendering.draw.GameRenderer
import com.example.einkarcade.ui.rendering.geom.BoardViewport

internal class LevelTransitionAnimation(
    private val renderer: GameRenderer,
    private val viewport: BoardViewport,
    private val oldTiles: List<List<Tile>>,
    private val newTiles: List<List<Tile>>,
    private val boxPositions: Set<Position>,
    private val playerPosition: Position,
    private val viewWidth: Int,
    private val viewHeight: Int
) : Animation {

    private val viewRect = Rect(0, 0, viewWidth, viewHeight)
    private val startTick = nowTick(SystemClock.elapsedRealtime())
    private val rows: Int = newTiles.size
    private val cols: Int = newTiles.firstOrNull()?.size ?: 0
    private val maxIndex: Int =
        if (rows == 0 || cols == 0) 0 else (rows - 1) + (cols - 1)

    private val flashBlackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = false
    }
    private val flashWhitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    override fun dirtyRect(): Rect? = viewRect

    override fun drawOverEntities(canvas: Canvas) {
        val nowMs = SystemClock.elapsedRealtime()
        renderer.drawBackground(canvas, viewWidth, viewHeight)
        drawTransitionTiles(canvas, nowMs)
        drawTransitionFlashOverlay(canvas, nowMs)
        drawTransitionEntities(canvas, nowMs)
    }

    override fun ticksUntilNextStep(): Int? {
        return if (isComplete(SystemClock.elapsedRealtime())) null else 1
    }

    override fun hidesBoard(): Boolean = true

    private fun drawTransitionTiles(canvas: Canvas, nowMs: Long) {
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val newTile = tileAt(newTiles, rowIndex, colIndex)
                val growScale = if (newTile != Tile.WALL) {
                    growScale(nowMs, rowIndex, colIndex)
                } else {
                    null
                }
                if (growScale != null) {
                    renderer.drawScaledTile(
                        canvas = canvas,
                        viewport = viewport,
                        tile = newTile,
                        rowIndex = rowIndex,
                        colIndex = colIndex,
                        scale = growScale
                    )
                }
            }
        }
    }

    private fun drawTransitionFlashOverlay(canvas: Canvas, nowMs: Long) {
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val newTile = tileAt(newTiles, rowIndex, colIndex)
                if (newTile == Tile.WALL) continue

                val phase = flashPhase(nowMs, rowIndex, colIndex) ?: continue
                val paint = when (phase) {
                    FlashPhase.BLACK -> flashBlackPaint
                    FlashPhase.WHITE -> flashWhitePaint
                }

                val left = offsetX + (colIndex + 1) * cellSize
                val top = offsetY + (rowIndex + 1) * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    private fun drawTransitionEntities(canvas: Canvas, nowMs: Long) {
        val readyBoxes = boxPositions.filter {
            isCellReady(nowMs, it.row, it.col)
        }.toSet()
        if (readyBoxes.isNotEmpty()) {
            renderer.drawBoxes(canvas, viewport, readyBoxes, selectedBox = null)
        }

        if (isCellReady(nowMs, playerPosition.row, playerPosition.col)) {
            renderer.drawPlayer(canvas, viewport, playerPosition)
        }
    }

    private fun isComplete(nowMs: Long): Boolean {
        val nowTick = nowTick(nowMs)
        val endTick = startTick + maxIndex * PER_TILE_DELAY_TICKS +
            totalDurationTicks() + FLASH_TOTAL_TICKS
        return nowTick >= endTick
    }

    private fun growScale(nowMs: Long, row: Int, col: Int): Float? {
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

    private fun flashPhase(nowMs: Long, row: Int, col: Int): FlashPhase? {
        val local = localElapsedTicks(nowMs, row, col)
        if (local < 0) return null
        val growDone = totalDurationTicks()
        if (local < growDone) return null
        val flashElapsed = local - growDone
        if (flashElapsed >= FLASH_TOTAL_TICKS) return null
        return if (flashElapsed < FLASH_BLACK_TICKS) {
            FlashPhase.BLACK
        } else {
            FlashPhase.WHITE
        }
    }

    private fun localElapsedTicks(nowMs: Long, row: Int, col: Int): Long {
        val nowTick = nowTick(nowMs)
        val index = (rows - 1 - row) + col
        return nowTick - (startTick + index * PER_TILE_DELAY_TICKS)
    }

    private fun totalDurationTicks(): Long {
        return kotlin.math.ceil(
            VanishSpec.totalDurationTicks().toDouble() * TRANSITION_DURATION_MULT
        ).toLong()
    }

    private fun tileAt(tiles: List<List<Tile>>, row: Int, col: Int): Tile {
        if (row < 0 || col < 0) return Tile.WALL
        val rowList = tiles.getOrNull(row) ?: return Tile.WALL
        return rowList.getOrNull(col) ?: Tile.WALL
    }

    private fun isCellReady(nowMs: Long, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0 || row >= rows || col >= cols) return false
        val local = localElapsedTicks(nowMs, row, col)
        return local >= totalDurationTicks() + FLASH_TOTAL_TICKS
    }

    private fun nowTick(nowMs: Long): Long = nowMs / ANIMATION_TICK_MS

    private enum class FlashPhase { BLACK, WHITE }

    private companion object {
        const val TRANSITION_DURATION_MULT: Float = 0.08f
        const val PER_TILE_DELAY_TICKS: Long = 2L
        const val FLASH_TOTAL_TICKS: Long = 2L
        const val FLASH_BLACK_TICKS: Long = 1L
        val GROW_SCALES = floatArrayOf(0.18f, 0.32f, 0.50f, 0.70f, 1.00f)
    }
}
