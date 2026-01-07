package com.example.einkarcade.ui.rendering

import android.graphics.Rect
import com.example.einkarcade.sokoban.Position

internal data class SpriteDrawParams(
    val left: Float,
    val top: Float,
    val sizePx: Int,
    val dirtyRect: Rect
)

internal fun spriteDrawParams(
    viewport: BoardViewport,
    position: Position,
    sizeFactor: Float
): SpriteDrawParams {
    val cellSize = viewport.cellSize
    val offsetX = viewport.offsetX
    val offsetY = viewport.offsetY

    val origin = Position(position.row + 1, position.col + 1)
        .toRenderPoint(cellSize, offsetX, offsetY)

    val targetSize = snapToWholePixel(cellSize * sizeFactor)
    val sizePx = targetSize.toInt().coerceAtLeast(1)
    val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
    val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)

    // Padding accounts for bitmap filtering, anti-aliasing, and pixel rounding.
    val paddingPx = 6
    val dirtyRect = Rect(
        left.toInt() - paddingPx,
        top.toInt() - paddingPx,
        (left + sizePx).toInt() + paddingPx,
        (top + sizePx).toInt() + paddingPx
    )

    return SpriteDrawParams(left = left, top = top, sizePx = sizePx, dirtyRect = dirtyRect)
}

internal fun computeBoxPathDirtyRect(
    viewport: BoardViewport,
    path: List<Position>,
    displayedPlayer: Position,
    pendingPlayer: Position
): Rect {
    require(path.size >= 2)

    val cellSize = viewport.cellSize
    val offsetX = viewport.offsetX
    val offsetY = viewport.offsetY

    val strokeWidth = cellSize * 0.2f
    val tailWidth = snapToWholePixel(cellSize * 0.90f)
    val tailHalfWidth = tailWidth / 2f
    val pad = maxOf(strokeWidth / 2f, tailHalfWidth) + 10f

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (position in path) {
        val cx = offsetX + (position.col + 1) * cellSize + cellSize / 2f
        val cy = offsetY + (position.row + 1) * cellSize + cellSize / 2f
        if (cx < minX) minX = cx
        if (cy < minY) minY = cy
        if (cx > maxX) maxX = cx
        if (cy > maxY) maxY = cy
    }

    val rect = Rect(
        (minX - pad).toInt(),
        (minY - pad).toInt(),
        (maxX + pad).toInt(),
        (maxY + pad).toInt()
    )

    // Include moved box sprite bounds (from/to).
    val from = path.first()
    val to = path.last()
    rect.union(spriteDrawParams(viewport, from, 0.90f).dirtyRect)
    rect.union(spriteDrawParams(viewport, to, 0.90f).dirtyRect)

    // Include player sprite bounds (both displayed during animation and pending at end).
    rect.union(spriteDrawParams(viewport, displayedPlayer, 0.80f).dirtyRect)
    rect.union(spriteDrawParams(viewport, pendingPlayer, 0.80f).dirtyRect)

    return rect
}

internal fun computeVanishDirtyRect(viewport: BoardViewport, position: Position): Rect {
    val cellSize = viewport.cellSize
    val offsetX = viewport.offsetX
    val offsetY = viewport.offsetY
    val left = offsetX + (position.col + 1) * cellSize
    val top = offsetY + (position.row + 1) * cellSize
    val right = left + cellSize
    val bottom = top + cellSize
    val paddingPx = 4f
    return Rect(
        (left - paddingPx).toInt(),
        (top - paddingPx).toInt(),
        (right + paddingPx).toInt(),
        (bottom + paddingPx).toInt()
    )
}
