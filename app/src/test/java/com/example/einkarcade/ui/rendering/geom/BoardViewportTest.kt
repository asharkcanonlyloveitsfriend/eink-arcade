package com.example.einkarcade.ui.rendering.geom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardViewportTest {
    @Test
    fun preservesHalfCellMarginsWhenTheyExceedTheUiMinimums() {
        val viewport =
            computeBoardViewport(
                surfaceWidth = 1000f,
                surfaceHeight = 800f,
                innerRows = 3,
                innerCols = 5,
                minimumTopMarginPx = 58f,
                minimumBottomMarginPx = 58f,
            )

        assertEquals(viewport.cellSize * BOARD_HORIZONTAL_MARGIN_CELLS, viewport.boardLeft, 0.01f)
        assertEquals(150f, viewport.boardTop, 0.01f)
        assertEquals(150f, 800f - viewport.boardBottom, 0.01f)
    }

    @Test
    fun preservesUiMinimumsAndRejectsTouchesInsideTheMargin() {
        val viewport =
            computeBoardViewport(
                surfaceWidth = 600f,
                surfaceHeight = 600f,
                innerRows = 5,
                innerCols = 5,
                minimumTopMarginPx = 58f,
                minimumBottomMarginPx = 58f,
            )

        assertTrue(viewport.boardTop >= 58f)
        assertTrue(600f - viewport.boardBottom >= 58f)
        assertNull(viewport.screenToInnerCell(viewport.boardLeft - 0.1f, viewport.boardTop))
    }
}
