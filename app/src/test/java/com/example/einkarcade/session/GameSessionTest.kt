package com.example.einkarcade.session

import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    @Test
    fun movePlayerToMovesAlongAnOpenPath() {
        val session = GameSession(Level.fromAscii("Test", "@  $."))

        assertTrue(session.movePlayerTo(Position(0, 1)))

        assertEquals(Position(0, 1), session.playerPosition)
        assertFalse(session.isAtStart)
    }

    @Test
    fun undoRestoresThePreviousBoxMove() {
        val session = GameSession(Level.fromAscii("Test", "@ $."))
        val path = listOf(Position(0, 2), Position(0, 3))
        session.moveBox(path.first(), path.last())

        assertTrue(session.undo())

        assertEquals(setOf(path.first()), session.boxPositions)
        assertEquals(Position(0, 1), session.playerPosition)
        assertFalse(session.undo())
    }

    @Test
    fun restartRestoresTheInitialSessionState() {
        val session = GameSession(Level.fromAscii("Test", "@  $."))
        assertTrue(session.movePlayerTo(Position(0, 1)))

        session.restart()

        assertTrue(session.isAtStart)
        assertEquals(Position(0, 0), session.playerPosition)
    }
}
