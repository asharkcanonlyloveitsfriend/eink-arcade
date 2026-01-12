package com.example.einkarcade.ui.rendering.draw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.ResolvedEntityGeometry
import com.example.einkarcade.ui.rendering.geom.toRenderPoint
import kotlin.math.ceil
import kotlin.math.floor

internal class GameRenderer(
    private val backgroundDrawer: BackgroundDrawer,
    private val tileDrawer: TileDrawer,
    private val entityDrawer: EntityDrawer
) {
    private var staticFrameBitmap: Bitmap? = null
    private var staticFrameOriginPx: Rect? = null

    fun rebuildStaticFrame(
        viewWidth: Int,
        viewHeight: Int,
        viewport: BoardViewport,
        tiles: List<List<Tile>>
    ) {
        val boardRect = computeBoardRectPx(viewport)

        // Render background + tiles, then crop to the board bounds.
        val fullBitmap = createBitmap(viewWidth, viewHeight)
        val fullCanvas = Canvas(fullBitmap)

        backgroundDrawer.draw(fullCanvas, viewWidth, viewHeight)
        tileDrawer.drawTiles(fullCanvas, viewport, tiles)

        val cropped = Bitmap.createBitmap(
            fullBitmap,
            boardRect.left,
            boardRect.top,
            boardRect.width(),
            boardRect.height()
        )

        staticFrameBitmap = cropped
        staticFrameOriginPx = boardRect
    }

    fun drawStaticFrame(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int
    ) {
        // Background first, then cached board frame.
        backgroundDrawer.draw(canvas, viewWidth, viewHeight)

        val origin = staticFrameOriginPx ?: error("Static frame origin not initialized")
        blitStaticFrame(canvas, origin)
    }

    fun drawEntities(
        canvas: Canvas,
        viewport: BoardViewport,
        geometry: ResolvedEntityGeometry,
        boxPositions: Set<Position>,
        playerPosition: Position,
        selectedBox: Position?
    ) {
        entityDrawer.drawBoxes(canvas, viewport, geometry, boxPositions)

        if (selectedBox != null) {
            entityDrawer.drawBox(
                canvas = canvas,
                viewport = viewport,
                geometry = geometry,
                position = selectedBox,
                resId = R.drawable.box_selected
            )
        }

        entityDrawer.drawPlayer(
            canvas = canvas,
            viewport = viewport,
            playerPosition = playerPosition
        )
    }

    fun computeBoxRect(
        viewport: BoardViewport,
        geometry: ResolvedEntityGeometry,
        position: Position
    ): Rect {
        val origin = Position(position.row + 1, position.col + 1)
            .toRenderPoint(viewport.cellSize, viewport.offsetX, viewport.offsetY)
        val bounds = geometry.boxBoundsPx

        val left = (origin.x + bounds.left).toInt() - 1
        val top = (origin.y + bounds.top).toInt() - 1
        val right = left + bounds.width() + 2
        val bottom = top + bounds.height() + 2

        return Rect(left, top, right, bottom)
    }

    fun computePlayerRect(
        viewport: BoardViewport,
        geometry: ResolvedEntityGeometry,
        position: Position
    ): Rect {
        val origin = Position(position.row + 1, position.col + 1)
            .toRenderPoint(viewport.cellSize, viewport.offsetX, viewport.offsetY)
        val bounds = geometry.playerBoundsPx

        val left = (origin.x + bounds.left).toInt() - 1
        val top = (origin.y + bounds.top).toInt() - 1
        val right = left + bounds.width() + 2
        val bottom = top + bounds.height() + 2

        return Rect(left, top, right, bottom)
    }

    fun computeDirtyRect(
        viewport: BoardViewport,
        geometry: ResolvedEntityGeometry,
        boxPositions: Iterable<Position> = emptyList(),
        playerPositions: Iterable<Position> = emptyList()
    ): Rect {
        var dirty: Rect? = null

        for (pos in boxPositions) {
            val r = computeBoxRect(viewport, geometry, pos)
            dirty = dirty?.apply { union(r) } ?: r
        }

        for (pos in playerPositions) {
            val r = computePlayerRect(viewport, geometry, pos)
            dirty = dirty?.apply { union(r) } ?: r
        }

        return dirty ?: error("computeDirtyRect called with no positions")
    }

    private fun computeBoardRectPx(viewport: BoardViewport): Rect {
        val rows = viewport.rows
        val cols = viewport.cols

        val left = floor(viewport.offsetX).toInt()
        val top = floor(viewport.offsetY).toInt()
        val right = ceil(viewport.offsetX + (viewport.cellSize * cols)).toInt()
        val bottom = ceil(viewport.offsetY + (viewport.cellSize * rows)).toInt()

        return Rect(left, top, right, bottom)
    }

    private fun blitStaticFrame(canvas: Canvas, dstRectPx: Rect) {
        val bitmap = staticFrameBitmap ?: error("Static frame bitmap not initialized")
        val origin = staticFrameOriginPx ?: error("Static frame origin not initialized")

        // Source rect is destination rect in cached-bitmap coordinates.
        val src = Rect(
            dstRectPx.left - origin.left,
            dstRectPx.top - origin.top,
            dstRectPx.right - origin.left,
            dstRectPx.bottom - origin.top
        )

        canvas.drawBitmap(bitmap, src, dstRectPx, null)
    }
}
