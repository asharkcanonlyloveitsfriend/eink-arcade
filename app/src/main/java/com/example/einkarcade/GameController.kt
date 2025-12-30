package com.example.einkarcade

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import com.example.einkarcade.appstate.LastSelectionStore
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.LevelsRepository
import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile

class GameController(
    context: Context,
    injectedSets: List<LevelSet>? = null,
    private val lastSelectionStore: LastSelectionStore = LastSelectionStore(context)
) {

    private val repository = LevelsRepository(context)
    private val fallbackLevelSet = LevelSet(
        id = "fallback",
        name = "No Levels",
        levels = listOf(Level.fromAscii("No Levels", "@"))
    )

    private val revisionState = mutableLongStateOf(0L)
    val revision: State<Long>
        get() = revisionState

    private fun markChanged() {
        revisionState.value = revisionState.value + 1L
    }

    private fun persistLevelChanges() {
        if (usingFallbackLevelSet) return
        Thread { repository.saveAllFromSets(levelSets) }.start()
    }

    private fun recordCompletionIfWon() {
        if (gameEngine.isGameWon) {
            level.markCompleted()
            persistLevelChanges()
        }
    }

    private fun loadLevelSets(): List<LevelSet>? = repository.loadSets()

    private val levelSets: List<LevelSet> = run {
        val sets = injectedSets ?: (loadLevelSets() ?: emptyList())
        val nonEmpty = sets.filter { it.levels.isNotEmpty() }
        if (nonEmpty.isEmpty()) listOf(fallbackLevelSet) else nonEmpty
    }
    private val usingFallbackLevelSet: Boolean = levelSets.size == 1 && levelSets[0].id == fallbackLevelSet.id

    private fun persistSelection() {
        if (usingFallbackLevelSet) return
        lastSelectionStore.save(levelSets[currentSetIndex].id, level.name)
    }

    val availableSetOptions: List<Pair<String, String>>
        get() = levelSets.map { it.id to it.name }

    fun selectSetById(setId: String) {
        val idx = levelSets.indexOfFirst { it.id == setId }
        if (idx == -1) return
        currentSetIndex = idx
        currentLevelIndex = 0
        level = levelsInCurrentSet[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
    }

    // Levels for current set.
    fun levels(): List<Level> = levelsInCurrentSet

    fun getCurrentRating(): Int = level.rating
    fun toggleThumbUp() {
        level.toggleThumbUp()
        persistLevelChanges()
        markChanged()
    }
    fun toggleThumbDown() {
        level.toggleThumbDown()
        persistLevelChanges()
        markChanged()
    }

    private var currentSetIndex: Int = 0
    private var currentLevelIndex = 0
    private lateinit var level: Level
    private lateinit var gameEngine: GameEngine
    val currentSetName: String
        get() = levelSets[currentSetIndex].name

    private val levelsInCurrentSet: List<Level>
        get() = levelSets[currentSetIndex].levels

    fun selectLevel(name: String) {
        val index = levelsInCurrentSet.indexOfFirst { it.name == name }
        if (index != -1) {
            currentLevelIndex = index
            level = levelsInCurrentSet[currentLevelIndex]
            gameEngine = GameEngine(level)
            persistSelection()
            markChanged()
        }
    }

    init {
        // Default to first set/level, then restore last selection if present.
        if (levelSets.isNotEmpty() && levelSets[0].levels.isNotEmpty()) {
            level = levelsInCurrentSet[currentLevelIndex]
            lastSelectionStore.load()?.let { (savedSetId, savedLevelName) ->
                val setIdx = levelSets.indexOfFirst { it.id == savedSetId }
                if (setIdx != -1) {
                    currentSetIndex = setIdx
                }
                val levelIdx = levelsInCurrentSet.indexOfFirst { it.name == savedLevelName }
                if (levelIdx != -1) {
                    currentLevelIndex = levelIdx
                }
                level = levelsInCurrentSet[currentLevelIndex]
            }
            gameEngine = GameEngine(level)
            // Persist selection once the controller is constructed.
            persistSelection()
        }
    }

    val playerPosition: Position
        get() = gameEngine.playerPosition

    val boxPositions: Set<Position>
        get() = gameEngine.boxPositions

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val tiles: List<List<Tile>>
        get() = level.grid

    val levelName: String
        get() = level.name

    fun restart() {
        if (isGameWon) {
            nextLevel()
            return
        }
        gameEngine = GameEngine(level)
        markChanged()
    }

    fun nextLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = (currentLevelIndex + 1) % levels.size
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
    }

    fun previousLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = if (currentLevelIndex - 1 < 0) levels.size - 1 else currentLevelIndex - 1
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
    }

    fun undo(): Boolean {
        val changed = gameEngine.undo()
        if (!changed) return false
        markChanged()
        return true
    }

    fun movePlayerTo(position: Position): Boolean {
        val changed = gameEngine.movePlayerTo(position)
        if (changed) {
            markChanged()
        }
        return changed
    }

    fun moveBoxTo(boxFrom: Position, boxTo: Position): Boolean {
        val changed = gameEngine.moveBoxTo(boxFrom, boxTo)
        if (!changed) return false
        recordCompletionIfWon()
        markChanged()
        return true
    }
}
