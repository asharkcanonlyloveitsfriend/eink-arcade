package com.example.einkarcade.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val DEFAULT_PAGE_SWIPE_DISTANCE = 32.dp

internal fun Modifier.verticalPageSwipe(
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    minimumDistance: Dp = DEFAULT_PAGE_SWIPE_DISTANCE,
): Modifier =
    pointerInput(onPreviousPage, onNextPage, minimumDistance) {
        val minimumDistancePx = minimumDistance.toPx()
        var dragDistancePx = 0f

        detectVerticalDragGestures(
            onDragStart = { dragDistancePx = 0f },
            onDragCancel = { dragDistancePx = 0f },
            onDragEnd = {
                when (verticalPageSwipeDirection(dragDistancePx, minimumDistancePx)) {
                    VerticalPageSwipeDirection.PREVIOUS -> onPreviousPage()
                    VerticalPageSwipeDirection.NEXT -> onNextPage()
                    null -> Unit
                }
                dragDistancePx = 0f
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                dragDistancePx += dragAmount
            },
        )
    }

internal enum class VerticalPageSwipeDirection {
    PREVIOUS,
    NEXT,
}

internal fun verticalPageSwipeDirection(
    dragDistancePx: Float,
    minimumDistancePx: Float,
): VerticalPageSwipeDirection? {
    require(minimumDistancePx > 0f)
    if (abs(dragDistancePx) < minimumDistancePx) return null
    return if (dragDistancePx < 0f) {
        VerticalPageSwipeDirection.NEXT
    } else {
        VerticalPageSwipeDirection.PREVIOUS
    }
}
