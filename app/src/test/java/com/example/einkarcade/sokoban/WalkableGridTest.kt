package com.example.einkarcade.sokoban

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkableGridTest {
    @Test
    fun obstaclesBecomeUnwalkableWithoutMutatingTheBaseGrid() {
        val base =
            arrayOf(
                arrayOf(true, true),
                arrayOf(false, true),
            )

        val result = WalkableGrid.withObstacles(base, setOf(Position(0, 1)))

        assertFalse(result[0][1])
        assertFalse(result[1][0])
        assertTrue(base[0][1])
        assertNotSame(base[0], result[0])
        assertArrayEquals(arrayOf(true, true), base[0])
    }
}
