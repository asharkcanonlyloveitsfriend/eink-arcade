package com.example.einkarcade.ui.modes

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelPickerPagingTest {
    @Test
    fun selectedLevelStartsInMiddleRowWhenPossible() {
        val selectedIndex = 7

        val pageStart = LevelPickerPaging.initialPageStart(selectedIndex, levelCount = 30)

        assertEquals(3, pageStart)
        assertEquals(1, (selectedIndex - pageStart) / LevelPickerPaging.GRID_SIZE)
    }

    @Test
    fun selectedLevelClampsToFirstAndLastPages() {
        assertEquals(0, LevelPickerPaging.initialPageStart(selectedIndex = 1, levelCount = 15))
        assertEquals(6, LevelPickerPaging.initialPageStart(selectedIndex = 14, levelCount = 15))
    }

    @Test
    fun selectedLevelMarkerTracksItsRowAcrossTheWholeCatalog() {
        assertEquals(0f, LevelPickerPaging.selectedRowScrollFraction(selectedIndex = 2, levelCount = 15), 0f)
        assertEquals(0.25f, LevelPickerPaging.selectedRowScrollFraction(selectedIndex = 3, levelCount = 15), 0f)
        assertEquals(0.5f, LevelPickerPaging.selectedRowScrollFraction(selectedIndex = 6, levelCount = 15), 0f)
        assertEquals(1f, LevelPickerPaging.selectedRowScrollFraction(selectedIndex = 14, levelCount = 15), 0f)
    }

    @Test
    fun lastPageOverlapsEarlierLevelsInsteadOfLeavingBlankRows() {
        val pageStart = LevelPickerPaging.lastPageStart(levelCount = 15)

        assertEquals(6, pageStart)
        assertEquals(6..14, pageStart until pageStart + LevelPickerPaging.PAGE_SIZE)
    }

    @Test
    fun pagingMovesNinePositionsAndRemainsReversibleFromCenteredPage() {
        val centeredPageStart = 3

        val nextPageStart =
            LevelPickerPaging.nextPageStart(centeredPageStart, levelCount = 30)

        assertEquals(12, nextPageStart)
        assertEquals(centeredPageStart, LevelPickerPaging.previousPageStart(nextPageStart))
    }

    @Test
    fun pagingClampsAtBothEnds() {
        assertEquals(0, LevelPickerPaging.previousPageStart(currentStart = 0))
        assertEquals(6, LevelPickerPaging.nextPageStart(currentStart = 6, levelCount = 15))
    }
}
