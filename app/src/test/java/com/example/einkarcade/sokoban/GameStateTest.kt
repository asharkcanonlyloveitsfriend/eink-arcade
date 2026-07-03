package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    @Test
    fun fromLevelCopiesInitialPositions() {
        val level = Level.fromAscii("test", "@ $.")

        val state = GameState.fromLevel(level)

        assertEquals(Position(0, 0), state.playerPosition)
        assertEquals(setOf(Position(0, 2)), state.boxPositions)
        assertNotSame(level.boxPositions, state.boxPositions)
    }

    @Test
    fun moveBoxReplacesTheOriginalPosition() {
        val state = GameState(Position(0, 0), mutableSetOf(Position(1, 1)))

        state.moveBox(Position(1, 1), Position(1, 2))

        assertFalse(state.hasBoxAt(Position(1, 1)))
        assertTrue(state.hasBoxAt(Position(1, 2)))
    }

    @Test
    fun moveBoxRejectsAnEmptySource() {
        val state = GameState(Position(0, 0), mutableSetOf())

        assertThrows(IllegalStateException::class.java) {
            state.moveBox(Position(1, 1), Position(1, 2))
        }
    }
}
