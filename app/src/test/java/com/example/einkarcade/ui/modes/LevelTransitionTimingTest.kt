package com.example.einkarcade.ui.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevelTransitionTimingTest {
    @Test
    fun flashPhaseStartsAfterBandAndGapThenAdvancesOncePerStep() {
        val stepS = 0.25f
        val bandS = 3f * stepS
        val gapS = 2f * stepS
        val completionS = 0.5f
        val firstFlashStep = computeFirstFlashStep(completionS, stepS, bandS, gapS)

        assertEquals(6, firstFlashStep)
        assertNull(computeFlashPhaseIndex(5, firstFlashStep))
        assertEquals(0, computeFlashPhaseIndex(6, firstFlashStep))
        assertEquals(1, computeFlashPhaseIndex(7, firstFlashStep))
        assertEquals(4, computeFlashPhaseIndex(10, firstFlashStep))
        assertEquals(5, computeFlashPhaseIndex(11, firstFlashStep))
    }
}
