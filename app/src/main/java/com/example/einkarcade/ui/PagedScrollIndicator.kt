@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
internal fun PagedScrollIndicator(
    scrollFraction: Float,
    visibleFraction: Float,
    selectedFraction: Float? = null,
    onScrollToStart: () -> Unit,
    onScrollToEnd: () -> Unit,
    onScrollToSelected: (() -> Unit)? = null,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedScrollFraction = scrollFraction.coerceIn(0f, 1f)
    val normalizedVisibleFraction = visibleFraction.coerceIn(0f, 1f)
    val normalizedSelectedFraction = selectedFraction?.coerceIn(0f, 1f)

    Canvas(
        modifier =
            modifier.pointerInput(
                normalizedScrollFraction,
                normalizedVisibleFraction,
                normalizedSelectedFraction,
                onScrollToStart,
                onScrollToEnd,
                onScrollToSelected,
                onPreviousPage,
                onNextPage,
            ) {
                detectTapGestures { tapOffset ->
                    val trackInset = 8.dp.toPx()
                    val selectedPosition =
                        normalizedSelectedFraction?.let {
                            calculateTrackPosition(
                                canvasHeight = size.height.toFloat(),
                                trackInset = trackInset,
                                fraction = it,
                            )
                        }
                    val thumb =
                        calculateThumb(
                            canvasHeight = size.height.toFloat(),
                            trackInset = trackInset,
                            scrollFraction = normalizedScrollFraction,
                            visibleFraction = normalizedVisibleFraction,
                            minimumHeight = 20.dp.toPx(),
                        )
                    val tipTapHeight = 16.dp.toPx()
                    val selectedTapRadius = 10.dp.toPx()
                    when {
                        selectedPosition != null &&
                            onScrollToSelected != null &&
                            tapOffset.y in
                                (selectedPosition - selectedTapRadius)..
                                    (selectedPosition + selectedTapRadius) -> onScrollToSelected()
                        tapOffset.y <= tipTapHeight -> onScrollToStart()
                        tapOffset.y >= size.height - tipTapHeight -> onScrollToEnd()
                        tapOffset.y < thumb.top -> onPreviousPage()
                        tapOffset.y > thumb.top + thumb.height -> onNextPage()
                    }
                }
            },
    ) {
        if (size.height <= 0f) return@Canvas

        val trackWidth = 2.dp.toPx()
        val thumbWidth = 8.dp.toPx()
        val selectedMarkerHalfWidth = thumbWidth / 2f
        val trackInset = 8.dp.toPx()
        val tipHalfWidth = 5.dp.toPx()
        val tipDepth = 5.dp.toPx()
        val centerX = size.width / 2f
        val thumb =
            calculateThumb(
                canvasHeight = size.height,
                trackInset = trackInset,
                scrollFraction = normalizedScrollFraction,
                visibleFraction = normalizedVisibleFraction,
                minimumHeight = 20.dp.toPx(),
            )

        drawLine(
            color = Color.Gray,
            start = Offset(centerX, trackInset),
            end = Offset(centerX, size.height - trackInset),
            strokeWidth = trackWidth,
        )
        normalizedSelectedFraction?.let {
            val selectedPosition =
                calculateTrackPosition(
                    canvasHeight = size.height,
                    trackInset = trackInset,
                    fraction = it,
                )
            drawLine(
                color = Color.White,
                start = Offset(centerX - selectedMarkerHalfWidth, selectedPosition),
                end = Offset(centerX + selectedMarkerHalfWidth, selectedPosition),
                strokeWidth = trackWidth,
            )
        }
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX - tipHalfWidth, tipDepth),
            end = Offset(centerX, 0f),
            strokeWidth = trackWidth,
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX, 0f),
            end = Offset(centerX + tipHalfWidth, tipDepth),
            strokeWidth = trackWidth,
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX - tipHalfWidth, size.height - tipDepth),
            end = Offset(centerX, size.height),
            strokeWidth = trackWidth,
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(centerX, size.height),
            end = Offset(centerX + tipHalfWidth, size.height - tipDepth),
            strokeWidth = trackWidth,
        )
        drawRect(
            color = Color.LightGray,
            topLeft = Offset(centerX - (thumbWidth / 2f), thumb.top),
            size = Size(thumbWidth, thumb.height),
        )
    }
}

private data class ScrollThumb(
    val top: Float,
    val height: Float,
)

private fun calculateThumb(
    canvasHeight: Float,
    trackInset: Float,
    scrollFraction: Float,
    visibleFraction: Float,
    minimumHeight: Float,
): ScrollThumb {
    val trackHeight = (canvasHeight - (trackInset * 2f)).coerceAtLeast(0f)
    val height =
        (trackHeight * visibleFraction)
            .coerceAtLeast(minimumHeight)
            .coerceAtMost(trackHeight)
    val top = trackInset + ((trackHeight - height) * scrollFraction)
    return ScrollThumb(top = top, height = height)
}

private fun calculateTrackPosition(
    canvasHeight: Float,
    trackInset: Float,
    fraction: Float,
): Float {
    val trackHeight = (canvasHeight - (trackInset * 2f)).coerceAtLeast(0f)
    return trackInset + (trackHeight * fraction.coerceIn(0f, 1f))
}
