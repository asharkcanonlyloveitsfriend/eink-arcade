package com.example.einkarcade.session

import com.example.einkarcade.appstate.SelectionStore
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.selection.DefaultLevelPolicy
import com.example.einkarcade.sokoban.Level

class LevelNavigator(
    sets: List<LevelSet>,
    private val selectionStore: SelectionStore,
) {
    private var levelSets: List<LevelSet> = sets.filter { it.levels.isNotEmpty() }
    private var currentSetIndex = 0
    private var currentLevelIndex = 0

    val hasLevels: Boolean
        get() = levelSets.isNotEmpty()

    val currentSet: LevelSet
        get() = levelSets[currentSetIndex]

    val currentLevel: Level
        get() = currentSet.levels[currentLevelIndex]

    val levelsInCurrentSet: List<Level>
        get() = currentSet.levels

    init {
        restoreSelection()
        if (hasLevels) persistSelection()
    }

    fun selectSet(setId: Int): Boolean {
        val setIndex = levelSets.indexOfFirst { it.id == setId }
        if (setIndex == -1) return false
        return select(setIndex, DefaultLevelPolicy.pickIndex(levelSets[setIndex].levels))
    }

    fun selectLevel(puzzleId: Int): Boolean {
        val levelIndex = levelsInCurrentSet.indexOfFirst { it.puzzleId == puzzleId }
        if (levelIndex == -1) return false
        return select(currentSetIndex, levelIndex)
    }

    fun selectNextLevel(): Boolean {
        val nextIndex = (currentLevelIndex + 1) % levelsInCurrentSet.size
        return select(currentSetIndex, nextIndex)
    }

    private fun select(
        setIndex: Int,
        levelIndex: Int,
    ): Boolean {
        if (setIndex !in levelSets.indices) return false
        if (levelIndex !in levelSets[setIndex].levels.indices) return false
        val previousPuzzleId = currentLevel.puzzleId
        currentSetIndex = setIndex
        currentLevelIndex = levelIndex
        persistSelection()
        return currentLevel.puzzleId != previousPuzzleId
    }

    private fun restoreSelection() {
        if (!hasLevels) return
        val (savedSetId, savedPuzzleId) = selectionStore.load()
        val savedSetIndex = levelSets.indexOfFirst { it.id == savedSetId }
        if (savedSetIndex == -1) return
        currentSetIndex = savedSetIndex
        val savedLevelIndex = levelsInCurrentSet.indexOfFirst { it.puzzleId == savedPuzzleId }
        if (savedLevelIndex != -1) currentLevelIndex = savedLevelIndex
    }

    private fun persistSelection() {
        selectionStore.save(currentSet.id, currentLevel.puzzleId)
    }
}
