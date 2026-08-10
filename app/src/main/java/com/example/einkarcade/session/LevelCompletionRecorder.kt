package com.example.einkarcade.session

import com.example.einkarcade.data.LevelDataSource

class LevelCompletionRecorder(
    private val dataSource: LevelDataSource,
) {
    enum class Result {
        NOT_SOLVED,
        CLEAN_SOLUTION,
        CHEAT_SOLUTION,
    }

    fun record(session: GameSession): Result {
        if (!session.isLevelSolved) return Result.NOT_SOLVED
        if (!session.isCleanSolution) return Result.CHEAT_SOLUTION

        val timestamp = dataSource.recordCompletion(session.level, session.boxMoveHistory)
        session.level.markCompleted(timestamp)
        return Result.CLEAN_SOLUTION
    }
}
