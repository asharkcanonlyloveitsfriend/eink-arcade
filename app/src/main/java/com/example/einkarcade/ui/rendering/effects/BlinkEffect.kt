package com.example.einkarcade.ui.rendering.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.draw.GameRenderer
import com.example.einkarcade.ui.rendering.geom.BoardViewport

internal class BlinkEffect(
    override val durationMs: Long,
    private val renderer: GameRenderer,
    private val viewport: BoardViewport,
    private val playerPos: Position
) : Effect {

    override fun dirtyRect(): Rect = eyesRect

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(eyesBitmap, null, spriteRect, null)
    }

    private val eyesRect: Rect by lazy {
        renderer.computePlayerEyesRect(viewport, playerPos)
    }

    private val eyesBitmap: Bitmap by lazy {
        renderer.getPlayerEyesBlinkBitmap()
    }

    private val spriteRect: Rect by lazy {
        renderer.computePlayerRect(viewport, playerPos)
    }
}
