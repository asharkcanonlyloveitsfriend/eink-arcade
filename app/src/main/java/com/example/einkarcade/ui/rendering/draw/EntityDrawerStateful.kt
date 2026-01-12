package com.example.einkarcade.ui.rendering.draw

import android.graphics.Canvas
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.snapToWholePixel
import com.example.einkarcade.ui.rendering.geom.toRenderPoint

internal class EntityDrawerStateful(private val assets: AndroidGameAssets) {
    fun drawBoxes(
        canvas: Canvas,
        viewport: BoardViewport,
        boxPositions: Set<Position>,
        selectedBox: Position?
    ) {
        val bitmapPaint = assets.bitmapPaint()
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY

        for (position in boxPositions) {
            val origin = Position(position.row + 1, position.col + 1)
                .toRenderPoint(cellSize, offsetX, offsetY)
            val targetSize = snapToWholePixel(cellSize * 0.90f)
            val sizePx = targetSize.toInt()
            require(sizePx > 0)
            val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
            val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
            val resId =
                if (selectedBox == position) R.drawable.box_selected else R.drawable.box
            val bitmap = assets.getBitmap(resId, sizePx)
            canvas.drawBitmap(bitmap, left, top, bitmapPaint)
        }
    }

    fun drawPlayer(
        canvas: Canvas,
        viewport: BoardViewport,
        playerPosition: Position,
        isFacingLeft: Boolean,
        blinkActive: Boolean
    ) {
        val bitmapPaint = assets.bitmapPaint()
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY

        val origin = Position(playerPosition.row + 1, playerPosition.col + 1)
            .toRenderPoint(cellSize, offsetX, offsetY)
        val targetSize = snapToWholePixel(cellSize * 0.80f)
        val sizePx = targetSize.toInt()
        require(sizePx > 0)
        val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
        val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
        val body = assets.getBitmap(R.drawable.player_slime, sizePx)
        drawSprite(canvas, body, left, top, sizePx, isFacingLeft, bitmapPaint)
        if (blinkActive) {
            val eyes = assets.getBitmap(R.drawable.player_eyes_blink, sizePx)
            drawSprite(canvas, eyes, left, top, sizePx, isFacingLeft, bitmapPaint)
        }
    }
}
