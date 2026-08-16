package com.example.einkarcade.ui.screens

import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationaryTapTrackerTest {
    private val tracker = StationaryTapTracker(timeoutMillis = 300)

    @Test
    fun identifiesTwoStationaryTapsOnTheSameCell() {
        val position = Position(1, 1)

        assertFalse(tracker.consumeMatchingTap(position, eventTimeMillis = 100))
        tracker.recordTap(position, eventTimeMillis = 100, causedEntityMove = false)

        assertTrue(tracker.consumeMatchingTap(position, eventTimeMillis = 250))
    }

    @Test
    fun doesNotIdentifyATapThatMovedAnEntityAsTheFirstOfADoubleTap() {
        val position = Position(1, 1)

        assertFalse(tracker.consumeMatchingTap(position, eventTimeMillis = 100))
        tracker.recordTap(position, eventTimeMillis = 100, causedEntityMove = true)

        assertFalse(tracker.consumeMatchingTap(position, eventTimeMillis = 250))
    }

    @Test
    fun doesNotIdentifyTapsOnDifferentCellsOrOutsideTheTimeout() {
        val first = Position(1, 1)
        val second = Position(1, 2)

        tracker.recordTap(first, eventTimeMillis = 100, causedEntityMove = false)
        assertFalse(tracker.consumeMatchingTap(second, eventTimeMillis = 250))

        tracker.recordTap(first, eventTimeMillis = 100, causedEntityMove = false)
        assertFalse(tracker.consumeMatchingTap(first, eventTimeMillis = 401))
    }
}
