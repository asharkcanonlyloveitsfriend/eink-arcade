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
    val endT = totalSegments.toFloat()
    val startT = endT * shrink.coerceIn(0f, 1f)
    val startSegment = startT.toInt().coerceIn(0, totalSegments - 1)
    val endSegment = endT.toInt().coerceIn(0, totalSegments)
    val startFraction = startT - startSegment
    val endFraction = endT - endSegment

    fun interpolateOffset(start: Offset, end: Offset, t: Float): Offset {
        return Offset(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t
        )
    }

    val startPoint = interpolateOffset(points[startSegment], points[startSegment + 1], startFraction)
    val endPoint = if (endSegment >= totalSegments) {
        points.last()
    } else {
        interpolateOffset(points[endSegment], points[endSegment + 1], endFraction)
    }

    val strokeWidth = cellSize * 0.2f
    val drawPoints = mutableListOf(startPoint)
    for (index in (startSegment + 1)..endSegment) {
        if (index in points.indices) {
            drawPoints.add(points[index])
        }
    }
    drawPoints.add(endPoint)

    if (drawPoints.size >= 2) {
        for (index in 0 until drawPoints.size - 1) {
            drawLine(
                color = Color(0xFFD3D3D3),
                start = drawPoints[index],
                end = drawPoints[index + 1],
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    } else {
        drawCircle(
            color = Color(0xFFD3D3D3),
            radius = strokeWidth / 2,
            center = drawPoints.first()
        )
    }
}
