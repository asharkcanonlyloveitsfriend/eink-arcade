package com.example.einkarcade.session

import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.LevelDataSource
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionServiceTest {
    @Test
    fun recordsAndMarksCleanSolution() {
        val dataSource = FakeDataSource()
        val session = GameSession(Level.fromAscii("Test", "@$.", 5))
        session.engine.moveBoxTo(Position(0, 1), Position(0, 2))

        val result = CompletionService(dataSource).record(session)

        assertEquals(CompletionService.Result.CLEAN_SOLUTION, result)
        assertEquals(5, dataSource.recordedPuzzleId)
        assertTrue(session.level.isCompleted)
    }

    @Test
    fun doesNotPersistUnsolvedLevel() {
        val dataSource = FakeDataSource()
        val session = GameSession(Level.fromAscii("Test", "@ $.", 5))

        assertEquals(CompletionService.Result.NOT_SOLVED, CompletionService(dataSource).record(session))
        assertEquals(null, dataSource.recordedPuzzleId)
    }

    private class FakeDataSource : LevelDataSource {
        var recordedPuzzleId: Int? = null
        override fun loadSets(): List<LevelSet>? = null
        override fun syncWithServer() = Unit
        override fun recordCompletion(level: Level, solutionHistory: List<List<Position>>): String {
            recordedPuzzleId = level.puzzleId
            return "timestamp"
        }
    }
}
