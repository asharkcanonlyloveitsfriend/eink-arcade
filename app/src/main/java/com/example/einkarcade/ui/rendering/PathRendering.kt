package com.example.einkarcade.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.einkarcade.sokoban.Position

fun DrawScope.drawBoxPathLine(
    isActive: Boolean,
    shrink: Float,
    path: List<Position>,
    cellSize: Float,
    offsetX: Float,
    offsetY: Float
) {
    if (!isActive) return
    require(path.size >= 2) { "Box path requires at least two points." }

    val points = path.map { position ->
        Offset(
            offsetX + (position.col + 1) * cellSize + cellSize / 2,
            offsetY + (position.row + 1) * cellSize + cellSize / 2
        )
    }

    val totalSegments = points.size - 1
    val startT = totalSegments.toFloat() * shrink.coerceIn(0f, 1f)
    val startSegment = startT.toInt().coerceIn(0, totalSegments - 1)
    val startFraction = startT - startSegment

    fun interpolateOffset(start: Offset, end: Offset, t: Float): Offset {
        return Offset(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t
        )
    }

    val startPoint = interpolateOffset(points[startSegment], points[startSegment + 1], startFraction)
    val strokeWidth = cellSize * 0.2f

    // Draw from the moving start point to the end of the path.
    var prev = startPoint
    var drewAnySegment = false
    for (index in (startSegment + 1) until points.size) {
        val next = points[index]
        drawLine(
            color = Color(0xFFD3D3D3),
            start = prev,
            end = next,
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        prev = next
        drewAnySegment = true
    }

    // If the path has fully shrunk (or effectively is a point), draw a dot.
    if (!drewAnySegment) {
        drawCircle(
            color = Color(0xFFD3D3D3),
            radius = strokeWidth / 2,
            center = startPoint
        )
    }
}
