package com.example.einkarcade.session

import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    @Test
    fun restartReplacesEngineAtInitialState() {
        val session = GameSession(Level.fromAscii("Test", "@  $."))
        assertTrue(session.engine.movePlayerTo(Position(0, 1)))
        assertFalse(session.engine.isAtStart)

        session.restart()

        assertTrue(session.engine.isAtStart)
        assertEquals(Position(0, 0), session.engine.playerPosition)
    }
}
