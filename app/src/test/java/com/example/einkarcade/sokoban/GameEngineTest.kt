package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GameEngineTest {
    private val firstBoxPosition = Position(1, 3)
    private val secondBoxPosition = Position(1, 4)
    private val thirdBoxPosition = Position(1, 5)
    private val fourthBoxPosition = Position(1, 6)

    @Test
    fun negativeUndoLimitIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            createEngine(maxUndosAllowed = -1)
        }
    }

    @Test
    fun undoIsUnavailableBeforeABoxMove() {
        val engine = createEngine(maxUndosAllowed = 1)

        assertNull(engine.undo())
    }

    @Test
    fun boxMovesAddUndoCreditsUpToTheLimit() {
        val engine = createEngine(maxUndosAllowed = 2)

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))
        assertNotNull(engine.moveBoxTo(secondBoxPosition, thirdBoxPosition))
        assertNotNull(engine.moveBoxTo(thirdBoxPosition, fourthBoxPosition))

        assertNotNull(engine.undo())
        assertNotNull(engine.undo())
        assertNull(engine.undo())
        assertEquals(setOf(secondBoxPosition), engine.boxPositions)
    }

    @Test
    fun boxMoveAfterUndoAddsANewUndoCredit() {
        val engine = createEngine(maxUndosAllowed = 1)

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))
        assertNotNull(engine.undo())
        assertNull(engine.undo())

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))

        assertNotNull(engine.undo())
        assertNull(engine.undo())
        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
    }

    private fun createEngine(maxUndosAllowed: Int): GameEngine =
        GameEngine(
            level =
                Level.fromAscii(
                    "Undo test",
                    """
                    #########
                    #@ $   .#
                    #########
                    """.trimIndent(),
                ),
            maxUndosAllowed = maxUndosAllowed,
        )
}
