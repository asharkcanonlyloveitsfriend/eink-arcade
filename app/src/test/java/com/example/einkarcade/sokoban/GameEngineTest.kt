package com.example.einkarcade.sokoban

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val firstBoxPosition = Position(1, 3)
    private val secondBoxPosition = Position(1, 4)
    private val thirdBoxPosition = Position(1, 5)

    @Test
    fun undoIsUnavailableBeforeABoxMove() {
        val engine = createSingleBoxHallwayEngine()

        assertNull(engine.undoLastMoveAt(firstBoxPosition))
    }

    @Test
    fun isAtStartIsFalseWhenTheBoardIsRestoredButBoxMoveHistoryRemains() {
        val level =
            Level.fromAscii(
                "History test",
                """
                #######
                #     #
                #  $  #
                #  @  #
                #     #
                #######
                """.trimIndent(),
            )
        val boxStart = Position(2, 3)
        val engine = GameEngine(level)

        assertMoved(engine.moveBox(boxStart, Position(2, 4)))
        assertMoved(engine.moveBox(Position(2, 4), boxStart))
        assertTrue(engine.movePlayerTo(level.playerStart))

        assertEquals(setOf(boxStart), engine.boxPositions)
        assertEquals(level.playerStart, engine.playerPosition)
        assertFalse(engine.getBoxMoveHistory().isEmpty())
        assertFalse(engine.isAtStart)
    }

    @Test
    fun consecutiveBoxMovesAreRecordedAndUndoneSeparately() {
        val engine = createSingleBoxHallwayEngine()

        assertMoved(engine.moveBox(firstBoxPosition, secondBoxPosition))
        assertMoved(engine.moveBox(secondBoxPosition, thirdBoxPosition))

        assertEquals(
            listOf(
                listOf(firstBoxPosition, secondBoxPosition),
                listOf(secondBoxPosition, thirdBoxPosition),
            ),
            engine.getBoxMoveHistory(),
        )

        assertEquals(
            listOf(secondBoxPosition, thirdBoxPosition),
            engine.undoLastMoveAt(thirdBoxPosition),
        )
        assertEquals(setOf(secondBoxPosition), engine.boxPositions)

        assertEquals(
            listOf(firstBoxPosition, secondBoxPosition),
            engine.undoLastMoveAt(secondBoxPosition),
        )
        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
    }

    @Test
    fun boxMoveAfterUndoCanBeUndoneAgain() {
        val engine = createSingleBoxHallwayEngine()

        assertMoved(engine.moveBox(firstBoxPosition, secondBoxPosition))
        assertNotNull(engine.undoLastMoveAt(secondBoxPosition))
        assertNull(engine.undoLastMoveAt(firstBoxPosition))

        assertMoved(engine.moveBox(firstBoxPosition, secondBoxPosition))

        assertNotNull(engine.undoLastMoveAt(secondBoxPosition))
        assertNull(engine.undoLastMoveAt(firstBoxPosition))
        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
    }

    @Test
    fun playerCanMoveThroughOpenFloorButNotThroughABox() {
        val engine = createSingleBoxHallwayEngine()

        assertTrue(engine.movePlayerTo(Position(1, 2)))
        assertEquals(Position(1, 2), engine.playerPosition)
        assertFalse(engine.movePlayerTo(firstBoxPosition))
    }

    @Test
    fun movingPlayerToCurrentPositionIsRejected() {
        val engine = createSingleBoxHallwayEngine()

        assertTrue(engine.movePlayerTo(Position(1, 2)))
        assertFalse(engine.movePlayerTo(Position(1, 2)))
    }

    @Test
    fun movingTheBoxOntoTheGoalSolvesTheLevel() {
        val engine = createSingleBoxHallwayEngine()

        val result = engine.moveBox(firstBoxPosition, Position(1, 7))

        assertEquals(
            GameEngine.BoxMoveResult.Moved(
                listOf(
                    Position(1, 3),
                    Position(1, 4),
                    Position(1, 5),
                    Position(1, 6),
                    Position(1, 7),
                ),
            ),
            result,
        )
        assertTrue(engine.isLevelSolved)
        assertTrue(engine.isCleanSolution)
        assertFalse(engine.isAtStart)
    }

    @Test
    fun playerCanMoveAfterLevelIsSolved() {
        val engine = createSingleBoxHallwayEngine()

        assertMoved(engine.moveBox(firstBoxPosition, Position(1, 7)))

        assertTrue(engine.movePlayerTo(Position(1, 1)))
    }

    @Test
    fun undoingASolvedLevelIsRejected() {
        val engine = createSingleBoxHallwayEngine()

        assertMoved(engine.moveBox(firstBoxPosition, Position(1, 7)))

        assertNull(engine.undoLastMoveAt(Position(1, 7)))
        assertEquals(setOf(Position(1, 7)), engine.boxPositions)
    }

    @Test
    fun movingABoxAfterTheLevelIsSolvedIsRejected() {
        val boxStart = Position(1, 3)
        val goal = Position(1, 5)
        val floorBeyondGoal = Position(1, 6)
        val engine =
            GameEngine(
                Level.fromAscii(
                    "Solved move test",
                    """
                    ########
                    #@ $ . #
                    ########
                    """.trimIndent(),
                ),
            )

        assertMoved(engine.moveBox(boxStart, goal))

        assertEquals(GameEngine.BoxMoveResult.Rejected, engine.moveBox(goal, floorBeyondGoal))
        assertEquals(setOf(goal), engine.boxPositions)
    }

    @Test
    fun movingABoxToAnUnreachableFloorIsRejected() {
        val engine = createSingleBoxHallwayEngine()

        assertEquals(
            GameEngine.BoxMoveResult.Rejected,
            engine.moveBox(firstBoxPosition, Position(1, 2)),
        )
    }

    @Test
    fun undoRestoresBoxPositionButLeavesPlayerBehindBox() {
        val engine = createSingleBoxHallwayEngine()

        val path = requireMovedPath(engine.moveBox(firstBoxPosition, secondBoxPosition))
        assertNull(engine.undoLastMoveAt(firstBoxPosition))
        assertEquals(setOf(secondBoxPosition), engine.boxPositions)

        assertEquals(path, engine.undoLastMoveAt(secondBoxPosition))

        assertEquals(setOf(firstBoxPosition), engine.boxPositions)
        assertEquals(Position(1, 2), engine.playerPosition)
        assertFalse(engine.isAtStart)
        assertEquals(emptyList<List<Position>>(), engine.getBoxMoveHistory())
        assertNull(engine.undoLastMoveAt(firstBoxPosition))
    }

    @Test
    fun movingAnAdjacentBoxIntoVoidRemovesIt() {
        val box = Position(1, 2)
        val void = Position(1, 3)
        val engine = createBoxAdjacentToVoidEngine()

        assertEquals(GameEngine.BoxMoveResult.Removed(void), engine.moveBox(box, void))

        assertEquals(emptySet<Position>(), engine.boxPositions)
        assertEquals(box, engine.playerPosition)
        assertEquals(listOf(listOf(box, void)), engine.getBoxMoveHistory())
        assertTrue(engine.isLevelSolved)
        assertFalse(engine.isCleanSolution)
    }

    @Test
    fun movingABoxIntoVoidCannotBeUndoneAfterItSolvesTheLevel() {
        val box = Position(1, 2)
        val void = Position(1, 3)
        val engine = createBoxAdjacentToVoidEngine()

        assertEquals(GameEngine.BoxMoveResult.Removed(void), engine.moveBox(box, void))
        assertNull(engine.undoLastMoveAt(void))

        assertEquals(emptySet<Position>(), engine.boxPositions)
        assertEquals(box, engine.playerPosition)
        assertFalse(engine.isAtStart)
    }

    @Test
    fun movingAnEmptySourceIntoVoidIsRejected() {
        val engine = createBoxAdjacentToVoidEngine()

        assertEquals(
            GameEngine.BoxMoveResult.Rejected,
            engine.moveBox(Position(1, 1), Position(1, 0)),
        )
    }

    @Test
    fun movingABoxToANonAdjacentVoidIsRejected() {
        val engine = createBoxAdjacentToVoidEngine()

        assertEquals(
            GameEngine.BoxMoveResult.Rejected,
            engine.moveBox(Position(1, 2), Position(1, 4)),
        )
    }

    private fun createSingleBoxHallwayEngine(): GameEngine =
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
        )

    private fun requireMovedPath(result: GameEngine.BoxMoveResult): List<Position> =
        requireNotNull(result as? GameEngine.BoxMoveResult.Moved).path

    private fun assertMoved(result: GameEngine.BoxMoveResult) {
        assertTrue(result is GameEngine.BoxMoveResult.Moved)
    }

    private fun createBoxAdjacentToVoidEngine(): GameEngine =
        GameEngine(
            Level.fromAscii(
                "Void push test",
                """
                #####
                #@$##
                #####
                """.trimIndent(),
            ),
        )
}
