package com.example.einkarcade.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerticalPageSwipeTest {
    @Test
    fun upwardSwipeMovesToNextPage() {
        assertEquals(
            VerticalPageSwipeDirection.NEXT,
            verticalPageSwipeDirection(dragDistancePx = -40f, minimumDistancePx = 32f),
        )
    }

    @Test
    fun downwardSwipeMovesToPreviousPage() {
        assertEquals(
            VerticalPageSwipeDirection.PREVIOUS,
            verticalPageSwipeDirection(dragDistancePx = 40f, minimumDistancePx = 32f),
        )
    }

    @Test
    fun shortDragDoesNotChangePage() {
        assertNull(verticalPageSwipeDirection(dragDistancePx = 31f, minimumDistancePx = 32f))
        assertNull(verticalPageSwipeDirection(dragDistancePx = -31f, minimumDistancePx = 32f))
    }
}
