package com.example.einkarcade

import com.example.einkarcade.appstate.SelectionStore
import com.example.einkarcade.catalog.LevelSetSummary
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.CompletionRecord
import com.example.einkarcade.data.LevelDataSource
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import org.junit.Assert.assertEquals
import org.junit.Test

class GameControllerTest {
    @Test
    fun levelSetSummariesInitiallyRepresentLoadedSets() {
        val controller = controller(initialSets = listOf(set(id = 1, name = "One", levelIds = listOf(11, 12))))

        assertEquals(
            listOf(LevelSetSummary(id = 1, name = "One", levelCount = 2, completedCount = 0)),
            controller.levelSetSummaries.value,
        )
    }

    @Test
    fun reloadLevelSetsPublishesUpdatedSummaries() {
        val dataSource = FakeDataSource()
        val controller = controller(initialSets = listOf(set(id = 1, name = "One", levelIds = listOf(11))), dataSource = dataSource)
        dataSource.sets = listOf(set(id = 2, name = "Two", levelIds = listOf(21, 22)))

        controller.reloadLevelSets()

        assertEquals(
            listOf(LevelSetSummary(id = 2, name = "Two", levelCount = 2, completedCount = 0)),
            controller.levelSetSummaries.value,
        )
    }

    @Test
    fun cleanCompletionUpdatesTheSetCompletedCount() {
        val controller =
            controller(
                initialSets =
                    listOf(
                        LevelSet(
                            id = 1,
                            name = "One",
                            levels = listOf(Level.fromAscii("Level 1", "@$.", puzzleId = 11)),
                        ),
                    ),
            )

        controller.moveBox(boxFrom = Position(0, 1), boxTo = Position(0, 2))

        assertEquals(1, controller.solvedBoxMoveCount)
        assertEquals(true, controller.wonWithNewBestSolution)
        assertEquals(1, controller.levelSetSummaries.value.single().completedCount)
    }

    @Test
    fun completionAndRestartUpdateBestSolutionState() {
        val dataSource = FakeDataSource(isNewBestSolution = false)
        val controller =
            controller(
                initialSets =
                    listOf(
                        LevelSet(
                            id = 1,
                            name = "One",
                            levels = listOf(Level.fromAscii("Level 1", "@$.", puzzleId = 11)),
                        ),
                    ),
                dataSource = dataSource,
            )

        controller.moveBox(boxFrom = Position(0, 1), boxTo = Position(0, 2))

        assertEquals(false, controller.wonWithNewBestSolution)

        dataSource.isNewBestSolution = true
        controller.restart()
        controller.moveBox(boxFrom = Position(0, 1), boxTo = Position(0, 2))
        assertEquals(true, controller.wonWithNewBestSolution)

        controller.restart()
        assertEquals(false, controller.wonWithNewBestSolution)
        assertEquals(0, controller.solvedBoxMoveCount)
    }

    private fun controller(
        initialSets: List<LevelSet>,
        dataSource: FakeDataSource = FakeDataSource(),
    ): GameController =
        GameController(
            selectionStore = FakeSelectionStore(),
            dataSource = dataSource,
            initialSets = initialSets,
        )

    private fun set(
        id: Int,
        name: String,
        levelIds: List<Int>,
    ): LevelSet =
        LevelSet(
            id = id,
            name = name,
            levels = levelIds.map { levelId -> Level.fromAscii("Level $levelId", "@ $.", puzzleId = levelId) },
        )

    private class FakeSelectionStore : SelectionStore {
        override fun save(
            setId: Int,
            puzzleId: Int,
        ) = Unit

        override fun load(): Pair<Int, Int> = 0 to 0
    }

    private class FakeDataSource(
        var isNewBestSolution: Boolean = true,
    ) : LevelDataSource {
        var sets: List<LevelSet> = emptyList()

        override fun loadSets(): List<LevelSet> = sets

        override fun recordCompletion(
            level: Level,
            solutionHistory: List<List<Position>>,
        ): CompletionRecord = CompletionRecord("completed", isNewBestSolution, boxMoveCount = 1)
    }
}
