package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val firstBoxPosition = Position(1, 3)
    private val secondBoxPosition = Position(1, 4)
    private val thirdBoxPosition = Position(1, 5)
    private val fourthBoxPosition = Position(1, 6)

    @Test
    fun constructorRejectsNegativeUndoLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            createSingleBoxHallwayEngine(maxUndosAllowed = -1)
        }
    }

    @Test
    fun undoIsUnavailableBeforeABoxMove() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        assertNull(engine.undo())
    }

    @Test
    fun boxMovesAddUndoCreditsUpToTheLimit() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 2)

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
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))
        assertNotNull(engine.undo())
        assertNull(engine.undo())

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))

        assertNotNull(engine.undo())
        assertNull(engine.undo())
        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
    }

    @Test
    fun playerCanMoveThroughOpenFloorButNotThroughABox() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        assertTrue(engine.movePlayerTo(Position(1, 2)))
        assertEquals(Position(1, 2), engine.playerPosition)
        assertFalse(engine.movePlayerTo(firstBoxPosition))
    }

    @Test
    fun movingPlayerToCurrentPositionIsRejected() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        assertTrue(engine.movePlayerTo(Position(1, 2)))
        assertFalse(engine.movePlayerTo(Position(1, 2)))
    }

    @Test
    fun movingTheBoxOntoTheGoalSolvesTheLevel() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        val path = engine.moveBoxTo(firstBoxPosition, Position(1, 7))

        assertEquals(
            listOf(
                Position(1, 3),
                Position(1, 4),
                Position(1, 5),
                Position(1, 6),
                Position(1, 7),
            ),
            path,
        )
        assertTrue(engine.isLevelSolved)
        assertTrue(engine.isCleanSolution)
        assertFalse(engine.isAtStart)
    }

    @Test
    fun playerCannotMoveAfterLevelIsSolved() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        assertNotNull(engine.moveBoxTo(firstBoxPosition, Position(1, 7)))

        assertFalse(engine.movePlayerTo(Position(1, 1)))
    }

    @Test
    fun undoRestoresBoxPositionButLeavesPlayerBehindBox() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 1)

        val path = requireNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))
        assertEquals(path, engine.undo())

        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
        assertEquals(Position(1, 2), engine.playerPosition)
        assertFalse(engine.isAtStart)
        assertEquals(emptyList<List<Position>>(), engine.getBoxMoveHistory())
        assertNull(engine.undo())
    }

    @Test
    fun zeroUndoLimitDoesNotAllowUndo() {
        val engine = createSingleBoxHallwayEngine(maxUndosAllowed = 0)

        assertNotNull(engine.moveBoxTo(firstBoxPosition, secondBoxPosition))

        assertNull(engine.undo())
    }

    @Test
    fun adjacentBoxCanBePushedIntoVoid() {
        val box = Position(1, 2)
        val void = Position(1, 3)
        val engine = createBoxAdjacentToVoidEngine()

        assertTrue(engine.pushBoxIntoVoid(box, void))

        assertEquals(emptySet<Position>(), engine.boxPositions)
        assertEquals(box, engine.playerPosition)
        assertEquals(listOf(listOf(box, void)), engine.getBoxMoveHistory())
        assertTrue(engine.isLevelSolved)
        assertFalse(engine.isCleanSolution)
    }

    @Test
    fun voidPushCanBeUndone() {
        val player = Position(1, 1)
        val box = Position(1, 2)
        val void = Position(1, 3)
        val engine = createBoxAdjacentToVoidEngine()

        assertTrue(engine.pushBoxIntoVoid(box, void))
        assertEquals(listOf(box, void), engine.undo())

        assertEquals(setOf(box), engine.boxPositions)
        assertEquals(player, engine.playerPosition)
        assertTrue(engine.isAtStart)
    }

    @Test
    fun voidPushRejectsEmptySource() {
        val engine = createBoxAdjacentToVoidEngine()

        assertFalse(engine.pushBoxIntoVoid(Position(1, 1), Position(1, 0)))
    }

    @Test
    fun voidPushRejectsNonAdjacentDestination() {
        val engine = createBoxAdjacentToVoidEngine()

        assertFalse(engine.pushBoxIntoVoid(Position(1, 2), Position(1, 4)))
    }

    @Test
    fun voidPushRejectsFloorDestination() {
        val box = Position(1, 2)

        val floorTargetEngine =
            GameEngine(
                Level.fromAscii(
                    "Floor target",
                    """
                    #####
                    #@$ #
                    #####
                    """.trimIndent(),
                ),
            )
        assertFalse(floorTargetEngine.pushBoxIntoVoid(box, Position(1, 3)))
    }

    @Test
    fun voidPushCannotBeUndoneWhenUndoLimitIsZero() {
        val engine = createBoxAdjacentToVoidEngine(maxUndosAllowed = 0)

        assertTrue(engine.pushBoxIntoVoid(Position(1, 2), Position(1, 3)))

        assertNull(engine.undo())
    }

    private fun createSingleBoxHallwayEngine(maxUndosAllowed: Int): GameEngine =
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

    private fun createBoxAdjacentToVoidEngine(maxUndosAllowed: Int = 1): GameEngine =
        GameEngine(
            Level.fromAscii(
                "Void push test",
                """
                #####
                #@$##
                #####
                """.trimIndent(),
            ),
            maxUndosAllowed = maxUndosAllowed,
        )
}
