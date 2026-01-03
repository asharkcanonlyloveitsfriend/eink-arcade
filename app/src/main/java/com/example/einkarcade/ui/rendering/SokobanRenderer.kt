package com.example.einkarcade.ui.rendering

import kotlin.math.roundToInt

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import com.example.einkarcade.sokoban.Position

const val CELL_SIZE = 100f
const val GRID_OFFSET_X = 50f
const val GRID_OFFSET_Y = 50f

// E-ink renders subpixel edges poorly.
private fun snapToWholePixel(px: Float): Float =
    px.roundToInt().toFloat()

fun DrawScope.drawGameObject(
    position: Position,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float,
    draw: DrawScope.(Offset) -> Unit
) {
    this.draw(position.toOffset(cellSize, offsetX, offsetY))
}

fun DrawScope.drawFloor(position: Position, cellSize: Float, offsetX: Float, offsetY: Float) {
    drawGameObject(position, cellSize, offsetX, offsetY) { offset ->
        drawRect(
            color = Color.White,
            topLeft = offset,
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFF0F0F0),
            topLeft = offset,
            size = Size(cellSize, cellSize),
            style = Stroke(width = 2f)
        )
    }
}

fun DrawScope.drawGoal(position: Position, cellSize: Float, offsetX: Float, offsetY: Float) {
    drawGameObject(position, cellSize, offsetX, offsetY) { offset ->
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = offset,
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White,
            topLeft = offset,
            size = Size(cellSize, cellSize),
            style = Stroke(width = 2f)
        )
    }
}

fun DrawScope.drawBox(
    position: Position,
    painter: Painter,
    selectedPainter: Painter,
    selected: Boolean,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float
) {
    val offset = position.toOffset(cellSize, offsetX, offsetY)
    val targetSize = snapToWholePixel(cellSize * 0.90f)
    val left = snapToWholePixel(offset.x + (cellSize - targetSize) / 2)
    val top = snapToWholePixel(offset.y + (cellSize - targetSize) / 2)
    val activePainter = if (selected) selectedPainter else painter

    // Draw box SVG
    withTransform({
        translate(left, top)
    }) {
        with(activePainter) {
            draw(size = Size(targetSize, targetSize))
        }
    }
}

fun DrawScope.drawPlayer(
    position: Position,
    painter: Painter,
    flipX: Boolean,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float
) {
    val offset = position.toOffset(cellSize, offsetX, offsetY)
    val targetSize = snapToWholePixel(cellSize * 0.8f)
    val left = snapToWholePixel(offset.x + (cellSize - targetSize) / 2)
    val top = snapToWholePixel(offset.y + (cellSize - targetSize) / 2)

    withTransform({
        translate(left, top)
        if (flipX) {
            scale(-1f, 1f, pivot = Offset(targetSize / 2f, targetSize / 2f))
        }
    }) {
        with(painter) {
            draw(size = Size(targetSize, targetSize))
        }
    }
}

fun Position.toOffset(
    cellSize: Float = CELL_SIZE,
    offsetX: Float = GRID_OFFSET_X,
    offsetY: Float = GRID_OFFSET_Y
): Offset {
    return Offset(
        offsetX + this.col * cellSize,
        offsetY + this.row * cellSize
    )
}
