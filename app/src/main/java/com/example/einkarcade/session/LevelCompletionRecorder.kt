package com.example.einkarcade.session

import com.example.einkarcade.data.LevelDataSource

class LevelCompletionRecorder(
    private val dataSource: LevelDataSource,
) {
    sealed interface Result {
        data object NotSolved : Result

        data class CleanSolution(
            val isNewBestSolution: Boolean,
            val boxMoveCount: Int,
        ) : Result

        data object CheatSolution : Result
    }

    fun record(session: GameSession): Result {
        if (!session.isLevelSolved) return Result.NotSolved
        if (!session.isCleanSolution) return Result.CheatSolution

        val completion = dataSource.recordCompletion(session.level, session.boxMoveHistory)
        session.level.markCompleted(completion.timestamp)
        return Result.CleanSolution(
            isNewBestSolution = completion.isNewBestSolution,
            boxMoveCount = completion.boxMoveCount,
        )
    }
}
