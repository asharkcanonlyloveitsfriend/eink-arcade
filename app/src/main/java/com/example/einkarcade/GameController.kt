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

    private val revisionState = mutableLongStateOf(0L)
    val revision: State<Long>
        get() = revisionState

    /**
     * Renderer deltas emitted when the game state changes.
     */
    sealed interface RenderDelta {
        data class LevelLoaded(
            val tiles: List<List<Tile>>,
            val playerPosition: Position,
            val boxPositions: Set<Position>
        ) : RenderDelta
        data class PlayerMoved(val to: Position) : RenderDelta
        data class BoxMoved(val path: List<Position>) : RenderDelta
        data class GameWon(val isClean: Boolean) : RenderDelta

        data object MoveRejected : RenderDelta
    }

    private fun currentLevelLoadedDelta(): RenderDelta.LevelLoaded = RenderDelta.LevelLoaded(
        tiles = tiles,
        playerPosition = playerPosition,
        boxPositions = boxPositions
    )

    /**
     * Optional sink for render deltas (e.g., a SurfaceView bridge).
     *
     * The controller must not reference any specific renderer type.
     */
    var onRenderDelta: ((RenderDelta) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(currentLevelLoadedDelta())
        }

    private fun markChanged() {
        revisionState.value = revisionState.value + 1L
    }

    private fun recordCompletionIfWon() {
        if (gameEngine.isCleanWin) {
            val timestamp = repository.updateLastCompletedAt(level)
            level.markCompleted(timestamp)
        }
    }

    private fun notifyIfWon() {
        if (gameEngine.isGameWon) {
            markChanged()
            onRenderDelta?.invoke(RenderDelta.GameWon(isClean = gameEngine.isCleanWin))
        }
    }

    private fun loadLevelSets(): List<LevelSet>? = repository.loadSets()

    private var levelSets: List<LevelSet> = emptyList()

    private fun persistSelection() {
        lastSelectionStore.save(levelSets[currentSetIndex].id, level.name)
    }

    val availableSetOptions: List<Pair<String, String>>
        get() = levelSets.map { it.id to it.name }

    fun selectSetById(setId: String) {
        val idx = levelSets.indexOfFirst { it.id == setId }
        if (idx == -1) return
        currentSetIndex = idx

        val levels = levelsInCurrentSet
        val firstIncompleteIndex = levels.indexOfFirst { !it.isCompleted }
        currentLevelIndex = if (firstIncompleteIndex != -1) firstIncompleteIndex else 0

        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
    }

    // Levels for current set.
    fun levels(): List<Level> = levelsInCurrentSet

    fun getCurrentRating(): Int = level.rating
    fun toggleThumbUp() {
        level.toggleThumbUp()
        repository.updateRating(level)
        markChanged()
    }
    fun toggleThumbDown() {
        level.toggleThumbDown()
        repository.updateRating(level)
        markChanged()
    }

    fun syncWithServer() {
        repository.syncWithServer()
        val sets = loadLevelSets() ?: emptyList()
        rebuildState(sets)
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
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
            onRenderDelta?.invoke(currentLevelLoadedDelta())
        }
    }

    init {
        val sets = injectedSets ?: (loadLevelSets() ?: emptyList())
        rebuildState(sets)
    }

    val playerPosition: Position
        get() = gameEngine.playerPosition

    val boxPositions: Set<Position>
        get() = gameEngine.boxPositions

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val isCleanWin: Boolean
        get() = gameEngine.isCleanWin

    val isAtStart: Boolean
        get() = gameEngine.isAtStart

    val tiles: List<List<Tile>>
        get() = level.grid

    val levelName: String
        get() = level.name

    fun restart() {
        gameEngine = GameEngine(level)
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
    }

    fun nextLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = (currentLevelIndex + 1) % levels.size
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
    }

    fun previousLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = if (currentLevelIndex - 1 < 0) levels.size - 1 else currentLevelIndex - 1
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
        persistSelection()
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
    }

    fun undo(): Boolean {
        val changed = gameEngine.undo()
        if (!changed) return false
        markChanged()
        onRenderDelta?.invoke(currentLevelLoadedDelta())
        return true
    }

    fun movePlayerTo(position: Position): Boolean {
        val changed = gameEngine.movePlayerTo(position)
        if (changed) {
            onRenderDelta?.invoke(RenderDelta.PlayerMoved(to = gameEngine.playerPosition))
        }
        return changed
    }

    fun moveBoxTo(boxFrom: Position, boxTo: Position): List<Position>? {
        val boxPath = gameEngine.moveBoxTo(boxFrom, boxTo)
        if (boxPath == null) {
            onRenderDelta?.invoke(RenderDelta.MoveRejected)
            return null
        }
        recordCompletionIfWon()
        onRenderDelta?.invoke(RenderDelta.BoxMoved(path = boxPath))
        notifyIfWon()
        return boxPath
    }

    private fun rebuildState(sets: List<LevelSet>) {
        val nonEmpty = sets.filter { it.levels.isNotEmpty() }
        levelSets = nonEmpty
        currentSetIndex = 0
        currentLevelIndex = 0
        if (levelSets.isEmpty()) return
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
        persistSelection()
    }
}
