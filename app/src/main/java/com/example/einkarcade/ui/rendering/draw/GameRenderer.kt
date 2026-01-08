package com.example.einkarcade.ui.rendering.draw

import android.graphics.Canvas
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.LevelTransition
import com.example.einkarcade.ui.rendering.geom.BoardViewport

internal data class RenderStateSnapshot(
    val tiles: List<List<Tile>>,
    val boxPositions: Set<Position>,
    val playerPosition: Position,
    val selectedBox: Position?,
    val isFacingLeft: Boolean
)

internal class GameRenderer(
    private val backgroundDrawer: BackgroundDrawer,
    private val tileDrawer: TileDrawer,
    private val entityDrawer: EntityDrawer,
    private val effectsDrawer: EffectsDrawer
) {
    fun drawScene(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
        viewport: BoardViewport,
        renderState: RenderStateSnapshot,
        transition: LevelTransition?,
        overlay: OverlayState?,
        nowMs: Long,
        drawPlayer: Boolean = true
    ) {
        backgroundDrawer.draw(canvas, viewWidth, viewHeight)

        if (transition != null) {
            drawTransitionTiles(canvas, viewport, transition, nowMs)
            drawTransitionEntities(canvas, viewport, transition, renderState, overlay, nowMs, drawPlayer)
            return
        }

        tileDrawer.drawTiles(canvas, viewport, renderState.tiles)

        if (overlay != null) {
            effectsDrawer.drawBoxPathLine(canvas, viewport, overlay, nowMs)
            if (overlay.vanishPosition != null) {
                effectsDrawer.drawVanishingBox(canvas, viewport, overlay)
            }
        }

        entityDrawer.drawBoxes(canvas, viewport, renderState.boxPositions, renderState.selectedBox)
        if (drawPlayer) {
            entityDrawer.drawPlayer(
                canvas = canvas,
                viewport = viewport,
                playerPosition = renderState.playerPosition,
                isFacingLeft = renderState.isFacingLeft,
                blinkActive = overlay?.blinkActive == true
            )
        }
    }

    fun drawStaticFrame(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
        viewport: BoardViewport,
        tiles: List<List<Tile>>
    ) {
        backgroundDrawer.draw(canvas, viewWidth, viewHeight)
        tileDrawer.drawTiles(canvas, viewport, tiles)
    }

    fun drawEntities(
        canvas: Canvas,
        viewport: BoardViewport,
        renderState: RenderStateSnapshot,
        drawPlayer: Boolean = true,
        blinkActive: Boolean = false
    ) {
        entityDrawer.drawBoxes(canvas, viewport, renderState.boxPositions, renderState.selectedBox)
        if (drawPlayer) {
            entityDrawer.drawPlayer(
                canvas = canvas,
                viewport = viewport,
                playerPosition = renderState.playerPosition,
                isFacingLeft = renderState.isFacingLeft,
                blinkActive = blinkActive
            )
        }
    }

    private fun drawTransitionTiles(
        canvas: Canvas,
        viewport: BoardViewport,
        transition: LevelTransition,
        nowMs: Long
    ) {
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        val rows = transition.rows
        val cols = transition.cols
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val newTile = transition.tileAt(transition.newTiles, rowIndex, colIndex)
                val growScale = if (newTile != Tile.WALL) transition.growScale(nowMs, rowIndex, colIndex) else null
                if (growScale != null) {
                    tileDrawer.drawScaledTile(
                        canvas = canvas,
                        tile = newTile,
                        rowIndex = rowIndex,
                        colIndex = colIndex,
                        scale = growScale,
                        cellSize = cellSize,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            }
        }
    }

    private fun drawTransitionEntities(
        canvas: Canvas,
        viewport: BoardViewport,
        transition: LevelTransition,
        renderState: RenderStateSnapshot,
        overlay: OverlayState?,
        nowMs: Long,
        drawPlayer: Boolean = true
    ) {
        for (position in renderState.boxPositions) {
            if (!transition.isCellReady(nowMs, position.row, position.col)) continue
            entityDrawer.drawBoxes(
                canvas = canvas,
                viewport = viewport,
                boxPositions = setOf(position),
                selectedBox = renderState.selectedBox
            )
        }

        if (drawPlayer) {
            val playerPos = renderState.playerPosition
            if (transition.isCellReady(nowMs, playerPos.row, playerPos.col)) {
                entityDrawer.drawPlayer(
                    canvas = canvas,
                    viewport = viewport,
                    playerPosition = playerPos,
                    isFacingLeft = renderState.isFacingLeft,
                    blinkActive = overlay?.blinkActive == true
                )
            }
        }
    }
}
