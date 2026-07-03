package com.example.einkarcade.session

import com.example.einkarcade.appstate.SelectionStore
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.sokoban.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelNavigatorTest {
    @Test
    fun restoresSavedSelectionAndPersistsChanges() {
        val store = FakeSelectionStore(2 to 21)
        val navigator = LevelNavigator(testSets(), store)

        assertEquals(21, navigator.currentLevel.puzzleId)
        assertTrue(navigator.selectLevel(22))
        assertEquals(2 to 22, store.saved)
    }

    @Test
    fun nextLevelWrapsAndReportsWhetherPuzzleChanged() {
        val navigator = LevelNavigator(testSets(), FakeSelectionStore(1 to 12))

        assertTrue(navigator.selectNextLevel())
        assertEquals(11, navigator.currentLevel.puzzleId)
        assertFalse(navigator.selectLevel(11))
    }

    @Test
    fun ignoresEmptySets() {
        val sets = listOf(LevelSet(1, "Empty", emptyList())) + testSets()
        val navigator = LevelNavigator(sets, FakeSelectionStore(0 to 0))

        assertTrue(navigator.hasLevels)
        assertEquals(1, navigator.currentSet.id)
    }

    private fun testSets() =
        listOf(
            LevelSet(1, "One", listOf(level(11), level(12))),
            LevelSet(2, "Two", listOf(level(21), level(22))),
        )

    private fun level(id: Int) = Level.fromAscii("L$id", "@ $.", id)

    private class FakeSelectionStore(initial: Pair<Int, Int>) : SelectionStore {
        var saved = initial

        override fun save(setId: Int, puzzleId: Int) {
            saved = setId to puzzleId
        }

        override fun load(): Pair<Int, Int> = saved
    }
}
