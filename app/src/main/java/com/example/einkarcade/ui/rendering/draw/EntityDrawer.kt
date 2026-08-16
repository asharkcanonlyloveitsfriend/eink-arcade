package com.example.einkarcade.ui.rendering.draw

import android.graphics.Canvas
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.ResolvedEntityGeometry
import com.example.einkarcade.ui.rendering.geom.toRenderPoint

internal class EntityDrawer(
    private val assets: AndroidGameAssets,
) {
    fun drawBoxes(
        canvas: Canvas,
        viewport: BoardViewport,
        geometry: ResolvedEntityGeometry,
        boxPositions: Set<Position>,
    ) {
        val bitmapPaint = assets.bitmapPaint()
        for (position in boxPositions) {
            drawBox(
                canvas = canvas,
                viewport = viewport,
                position = position,
                resId = R.drawable.box,
                geometry = geometry,
                bitmapPaint = bitmapPaint,
            )
        }
    }

    fun drawBox(
        canvas: Canvas,
        viewport: BoardViewport,
        position: Position,
        resId: Int,
        geometry: ResolvedEntityGeometry,
        bitmapPaint: android.graphics.Paint = assets.bitmapPaint(),
    ) {
        val origin =
            position.toRenderPoint(viewport.cellSize, viewport.boardLeft, viewport.boardTop)

        val bounds = geometry.boxBoundsPx
        val left = origin.x + bounds.left
        val top = origin.y + bounds.top

        val bitmap = assets.getBitmap(resId, geometry.boxSizePx)
        canvas.drawBitmap(bitmap, left, top, bitmapPaint)
    }

    fun drawPlayer(
        canvas: Canvas,
        viewport: BoardViewport,
        playerPosition: Position,
        geometry: ResolvedEntityGeometry,
    ) {
        val bitmapPaint = assets.bitmapPaint()
        val origin =
            playerPosition.toRenderPoint(viewport.cellSize, viewport.boardLeft, viewport.boardTop)

        val left = origin.x + geometry.playerInsetPx
        val top = origin.y + geometry.playerInsetPx

        val body = assets.getBitmap(R.drawable.player_slime, geometry.playerSizePx)
        canvas.drawBitmap(body, left, top, bitmapPaint)
    }
}
