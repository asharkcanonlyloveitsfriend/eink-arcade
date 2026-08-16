package com.example.einkarcade.ui.rendering.geom

import com.example.einkarcade.sokoban.Position
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal const val BOARD_HORIZONTAL_MARGIN_CELLS = 0.5f
internal const val BOARD_VERTICAL_MARGIN_CELLS = 0.5f

data class BoardViewport(
    val innerRows: Int,
    val innerCols: Int,
    val cellSize: Float,
    val boardLeft: Float,
    val boardTop: Float,
) {
    val boardRight: Float
        get() = boardLeft + innerCols * cellSize
    val boardBottom: Float
        get() = boardTop + innerRows * cellSize

    fun cellLeft(col: Int): Float = boardLeft + col * cellSize

    fun cellTop(row: Int): Float = boardTop + row * cellSize
}

internal fun computeBoardViewport(
    surfaceWidth: Float,
    surfaceHeight: Float,
    innerRows: Int,
    innerCols: Int,
    minimumTopMarginPx: Float = 0f,
    minimumBottomMarginPx: Float = 0f,
): BoardViewport {
    require(innerRows > 0 && innerCols > 0)
    require(surfaceWidth > 0f && surfaceHeight > 0f)
    require(minimumTopMarginPx >= 0f && minimumBottomMarginPx >= 0f)
    require(minimumTopMarginPx + minimumBottomMarginPx < surfaceHeight)

    val horizontalCellLimit = surfaceWidth / (innerCols + 2 * BOARD_HORIZONTAL_MARGIN_CELLS)
    val verticalCellLimit =
        findLargestFittingCellSize(
            surfaceHeight = surfaceHeight,
            innerRows = innerRows,
            minimumTopMarginPx = minimumTopMarginPx,
            minimumBottomMarginPx = minimumBottomMarginPx,
            upperBound = horizontalCellLimit,
        )
    val cellSize = min(horizontalCellLimit, verticalCellLimit)
    val topMargin = max(minimumTopMarginPx, BOARD_VERTICAL_MARGIN_CELLS * cellSize)
    val bottomMargin = max(minimumBottomMarginPx, BOARD_VERTICAL_MARGIN_CELLS * cellSize)
    val renderedWidth = cellSize * (innerCols + 2 * BOARD_HORIZONTAL_MARGIN_CELLS)
    val renderedHeight = topMargin + innerRows * cellSize + bottomMargin

    return BoardViewport(
        innerRows = innerRows,
        innerCols = innerCols,
        cellSize = cellSize,
        boardLeft = (surfaceWidth - renderedWidth) / 2f + BOARD_HORIZONTAL_MARGIN_CELLS * cellSize,
        boardTop = topMargin + (surfaceHeight - renderedHeight) / 2f,
    )
}

private fun findLargestFittingCellSize(
    surfaceHeight: Float,
    innerRows: Int,
    minimumTopMarginPx: Float,
    minimumBottomMarginPx: Float,
    upperBound: Float,
): Float {
    var low = 0f
    var high = upperBound
    repeat(32) {
        val candidate = (low + high) / 2f
        val occupiedHeight =
            innerRows * candidate +
                max(minimumTopMarginPx, BOARD_VERTICAL_MARGIN_CELLS * candidate) +
                max(minimumBottomMarginPx, BOARD_VERTICAL_MARGIN_CELLS * candidate)
        if (occupiedHeight <= surfaceHeight) low = candidate else high = candidate
    }
    return low
}

internal fun BoardViewport.screenToInnerCell(
    x: Float,
    y: Float,
): Position? {
    val innerCol = floor((x - boardLeft) / cellSize).toInt()
    val innerRow = floor((y - boardTop) / cellSize).toInt()
    if (innerRow !in 0 until innerRows || innerCol !in 0 until innerCols) return null
    return Position(innerRow, innerCol)
}
