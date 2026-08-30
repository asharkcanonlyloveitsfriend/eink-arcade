package com.example.einkarcade.session

import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.CompletionRecord
import com.example.einkarcade.data.LevelDataSource
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCompletionRecorderTest {
    @Test
    fun recordsAndMarksCleanSolution() {
        val dataSource = FakeDataSource()
        val session = GameSession(Level.fromAscii("Test", "@$.", 5))
        session.moveBox(Position(0, 1), Position(0, 2))

        val result = LevelCompletionRecorder(dataSource).record(session)

        assertEquals(
            LevelCompletionRecorder.Result.CleanSolution(isNewBestSolution = true, boxMoveCount = 1),
            result,
        )
        assertEquals(5, dataSource.recordedPuzzleId)
        assertTrue(session.level.isCompleted)
    }

    @Test
    fun doesNotPersistUnsolvedLevel() {
        val dataSource = FakeDataSource()
        val session = GameSession(Level.fromAscii("Test", "@ $.", 5))

        assertEquals(
            LevelCompletionRecorder.Result.NotSolved,
            LevelCompletionRecorder(dataSource).record(session),
        )
        assertEquals(null, dataSource.recordedPuzzleId)
    }

    @Test
    fun reportsWhenCleanSolutionDoesNotImproveTheBestSolution() {
        val dataSource = FakeDataSource(isNewBestSolution = false)
        val session = GameSession(Level.fromAscii("Test", "@$.", 5))
        session.moveBox(Position(0, 1), Position(0, 2))

        assertEquals(
            LevelCompletionRecorder.Result.CleanSolution(isNewBestSolution = false, boxMoveCount = 1),
            LevelCompletionRecorder(dataSource).record(session),
        )
    }

    private class FakeDataSource(
        private val isNewBestSolution: Boolean = true,
    ) : LevelDataSource {
        var recordedPuzzleId: Int? = null

        override fun loadSets(): List<LevelSet>? = null

        override fun recordCompletion(
            level: Level,
            solutionHistory: List<List<Position>>,
        ): CompletionRecord {
            recordedPuzzleId = level.puzzleId
            return CompletionRecord("timestamp", isNewBestSolution, boxMoveCount = 1)
        }
    }
}
