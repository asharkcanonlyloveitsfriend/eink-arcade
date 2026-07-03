package com.example.einkarcade.session

import com.example.einkarcade.data.LevelDataSource

class CompletionService(
    private val dataSource: LevelDataSource,
) {
    enum class Result {
        NOT_SOLVED,
        CLEAN_SOLUTION,
        CHEAT_SOLUTION,
    }

    fun record(session: GameSession): Result {
        val engine = session.engine
        if (!engine.isLevelSolved) return Result.NOT_SOLVED
        if (!engine.isCleanSolution) return Result.CHEAT_SOLUTION

        val timestamp = dataSource.recordCompletion(session.level, engine.getBoxMoveHistory())
        session.level.markCompleted(timestamp)
        return Result.CLEAN_SOLUTION
    }
}
