package com.example.einkarcade.ui.rendering

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

fun DrawScope.drawGameObject(
    position: Position,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float,
    draw: DrawScope.(Offset) -> Unit
) {
    this.draw(position.toOffset(cellSize, offsetX, offsetY))
}

fun DrawScope.drawWall(position: Position) {
    drawGameObject(position, CELL_SIZE, GRID_OFFSET_X, GRID_OFFSET_Y) { offset ->
        // Solid black background
        drawRect(
            color = Color.Black,
            topLeft = offset,
            size = Size(CELL_SIZE, CELL_SIZE)
        )
        val seed = position.row * 73856093 xor position.col * 19349663

        // Star density: roughly ~2× previous, but still sparse
        val starCount = when ((seed and 0x7) % 5) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 0
        }

        // Cap: never draw more than 1 large star per tile
        var largeStarUsed = false

        for (i in 0 until starCount) {
            // Deterministic pseudo-random offsets (spread, not linear)
            val rx = ((seed shr (i * 3)) and 0xF) / 16f
            val ry = ((seed shr (i * 5)) and 0xF) / 16f

            val x = offset.x + CELL_SIZE * (0.15f + rx * 0.7f)
            val y = offset.y + CELL_SIZE * (0.12f + ry * 0.7f)

            // Star size: mostly small, occasional medium, rare large
            val size = when {
                !largeStarUsed && (seed shr (i * 7) and 0x3F) == 0 -> {
                    largeStarUsed = true
                    4f               // rare large star
                }
                (seed shr (i * 4) and 0x3) == 0 -> 3f   // medium stars more common
                else -> 2f                              // small stars
            }

            val color =
                if (size == 2f && (seed shr (i * 6) and 0x1) == 0) {
                    Color.White          // small bright stars
                } else if ((seed shr (i * 6) and 0x3) == 0) {
                    Color.White          // some medium bright stars
                } else {
                    Color(0xFFDDDDDD)    // dim stars
                }

            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(size, size)
            )
        }
    }
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
    selected: Boolean,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float
) {
    val offset = position.toOffset(cellSize, offsetX, offsetY)
    val targetSize = cellSize * 0.90f
    val left = offset.x + (cellSize - targetSize) / 2
    val top = offset.y + (cellSize - targetSize) / 2

    // Draw box SVG
    withTransform({
        translate(left, top)
    }) {
        with(painter) {
            draw(size = Size(targetSize, targetSize))
        }
    }

    if (selected) {
        val bracketLength = targetSize * 0.24f
        val strokeWidth = targetSize * 0.065f
        val inset = targetSize * 0.075f

        val x0 = left + inset
        val y0 = top + inset
        val x1 = left + targetSize - inset - bracketLength
        val y1 = top + targetSize - inset - bracketLength

        val color = Color.Black

        // Top-left
        drawRect(color, Offset(x0, y0), Size(bracketLength, strokeWidth))
        drawRect(color, Offset(x0, y0), Size(strokeWidth, bracketLength))

        // Top-right
        drawRect(color, Offset(x1, y0), Size(bracketLength, strokeWidth))
        drawRect(color, Offset(x1 + bracketLength - strokeWidth, y0), Size(strokeWidth, bracketLength))

        // Bottom-left
        drawRect(color, Offset(x0, y1 + bracketLength - strokeWidth), Size(bracketLength, strokeWidth))
        drawRect(color, Offset(x0, y1), Size(strokeWidth, bracketLength))

        // Bottom-right
        drawRect(
            color,
            Offset(x1, y1 + bracketLength - strokeWidth),
            Size(bracketLength, strokeWidth)
        )
        drawRect(
            color,
            Offset(x1 + bracketLength - strokeWidth, y1),
            Size(strokeWidth, bracketLength)
        )
    }
}

fun DrawScope.drawPlayer(
    position: Position,
    painter: Painter,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float
) {
    val offset = position.toOffset(cellSize, offsetX, offsetY)
    val targetSize = cellSize * 0.8f
    val left = offset.x + (cellSize - targetSize) / 2
    val top = offset.y + (cellSize - targetSize) / 2

    withTransform({
        translate(left, top)
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
