package com.example.einkarcade.ui.rendering.anim

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.withSave
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.geom.BoardViewport


private const val TILE_FLASH_BLACK_TICKS = 2
private const val TILE_FLASH_WHITE_TICKS = 2

private enum class FlickerFrame {
    BACKGROUND,
    OLD,
    NEW
}

private data class FlickerPhase(
    val frame: FlickerFrame,
    val ticks: Int
)

private val FLICKER_PHASES: List<FlickerPhase> = listOf(
    FlickerPhase(frame = FlickerFrame.BACKGROUND, ticks = 5),
    FlickerPhase(frame = FlickerFrame.NEW, ticks = 2),
    FlickerPhase(frame = FlickerFrame.BACKGROUND, ticks = 4),
    FlickerPhase(frame = FlickerFrame.NEW, ticks = 6)
)

internal class LevelTransitionAnimation(
    private val backgroundBitmap: Bitmap,
    private val oldBitmap: Bitmap,
    private val newBitmap: Bitmap,
    private val newViewport: BoardViewport,
    private val newTiles: List<List<Tile>>
) : Animation {

    override fun hidesBoard(): Boolean = true

    override fun dirtyRects(): Array<Rect?> = arrayOf(fullRect)

    private var flickerPhaseIndex = 0
    private var tileFlashPhase = 0 // 0 = black, 1 = white, >= 2 = done

    private val flashBlackPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = false
    }

    private val flashWhitePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = false
    }

    private fun isDone(): Boolean {
        val flickerDone = flickerPhaseIndex >= FLICKER_PHASES.size
        val tileFlashDone = tileFlashPhase >= 2
        return flickerDone && tileFlashDone
    }

    override fun drawOverEntities(canvas: Canvas) {
        if (isDone()) return

        // Stage 1: full-frame flicker.
        if (flickerPhaseIndex < FLICKER_PHASES.size) {
            val phase = FLICKER_PHASES[flickerPhaseIndex]
            drawFlickerFrame(canvas, phase.frame)
            flickerPhaseIndex++
            return
        }

        // Stage 2: draw the new frame, then overlay tile flashes (black, then white).
        canvas.drawBitmap(newBitmap, 0f, 0f, null)

        val paint = when (tileFlashPhase) {
            0 -> flashBlackPaint
            1 -> flashWhitePaint
            else -> null
        }

        if (paint != null) {
            for (r in flashTileRects) {
                canvas.withSave {
                    clipRect(r)
                    canvas.drawRect(r, paint)
                }
            }
        }

        tileFlashPhase++
    }

    override fun ticksUntilNextStep(): Int? {
        if (isDone()) return null

        if (flickerPhaseIndex < FLICKER_PHASES.size) {
            return FLICKER_PHASES[flickerPhaseIndex].ticks
        }

        return when (tileFlashPhase) {
            0 -> TILE_FLASH_BLACK_TICKS
            1 -> TILE_FLASH_WHITE_TICKS
            else -> null
        }
    }

    private fun drawFlickerFrame(canvas: Canvas, frame: FlickerFrame) {
        when (frame) {
            FlickerFrame.BACKGROUND -> canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
            FlickerFrame.OLD -> canvas.drawBitmap(oldBitmap, 0f, 0f, null)
            FlickerFrame.NEW -> canvas.drawBitmap(newBitmap, 0f, 0f, null)
        }
    }

    private val fullRect: Rect by lazy {
        Rect(0, 0, newBitmap.width, newBitmap.height)
    }

    // Rects for all new FLOOR/GOAL tiles, used for the final black/white flash cleanup.
    private val flashTileRects: List<Rect> by lazy {
        val out = mutableListOf<Rect>()
        val cell = newViewport.cellSize
        val cellInt = cell.toInt()

        for (r in newTiles.indices) {
            val row = newTiles[r]
            for (c in row.indices) {
                val t = row[c]
                if (t == Tile.FLOOR || t == Tile.GOAL) {
                    val left = (newViewport.offsetX + (c + 1) * cell).toInt()
                    val top = (newViewport.offsetY + (r + 1) * cell).toInt()
                    out.add(Rect(left, top, left + cellInt, top + cellInt))
                }
            }
        }

        out
    }
}