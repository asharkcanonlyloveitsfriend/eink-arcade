package com.example.einkarcade

import android.content.Context
import android.graphics.Canvas
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.graphics.createBitmap
import com.example.einkarcade.appstate.LastSelectionStore
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.LevelsRepository
import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.StaticBoardFrame
import com.example.einkarcade.ui.rendering.draw.BackgroundDrawer
import com.example.einkarcade.ui.rendering.draw.EntityDrawer
import com.example.einkarcade.ui.rendering.draw.GameRenderer
import com.example.einkarcade.ui.rendering.draw.TileDrawer
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport

class GameController(
    context: Context,
    injectedSets: List<LevelSet>? = null,
    private val lastSelectionStore: LastSelectionStore = LastSelectionStore(context),
) {
    private val repository = LevelsRepository(context)
    private val revisionState = mutableLongStateOf(0L)

    private var levelSets: List<LevelSet> = emptyList()
    private var currentSetIndex: Int = 0
    private var currentLevelIndex: Int = 0
    private lateinit var level: Level
    private lateinit var gameEngine: GameEngine

    enum class UiMode {
        GAMEPLAY,
        LEVEL_TRANSITION,
    }

    private val uiModeState = mutableLongStateOf(UiMode.GAMEPLAY.ordinal.toLong())

    val uiMode: UiMode
        get() = UiMode.entries[uiModeState.longValue.toInt()]

    private var pendingLevelIndex: Int? = null
    private var pendingSetIndex: Int? = null

    sealed interface RenderDelta {
        data class LevelLoaded(
            val staticFrame: StaticBoardFrame,
            val playerPosition: Position,
            val boxPositions: Set<Position>,
        ) : RenderDelta

        data class StateChanged(
            val playerPosition: Position,
            val boxPositions: Set<Position>,
            val annotation: StateChangeAnnotation? = null,
        ) : RenderDelta

        sealed interface StateChangeAnnotation {
            data object Undo : StateChangeAnnotation

            data object Restart : StateChangeAnnotation

            data object PlayerMoved : StateChangeAnnotation

            data class BoxRemoved(
                val position: Position,
            ) : StateChangeAnnotation

            data class BoxMoved(
                val path: List<Position>,
            ) : StateChangeAnnotation
        }

        data class GameWon(
            val isClean: Boolean,
        ) : RenderDelta

        data object MoveRejected : RenderDelta
    }

    val revision: State<Long>
        get() = revisionState

    var onRenderDelta: ((RenderDelta) -> Unit)? = null
        set(value) {
            field = value
            // Initial LevelLoaded is now emitted explicitly by the UI once a StaticBoardFrame exists.
        }

    val currentSetName: String
        get() = levelSets[currentSetIndex].name

    val availableSetOptions: List<Pair<Int, String>>
        get() = levelSets.map { it.id to it.name }

    val playerPosition: Position
        get() = gameEngine.playerPosition

    val boxPositions: Set<Position>
        get() = gameEngine.boxPositions

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val isAtStart: Boolean
        get() = gameEngine.isAtStart

    val tileMap: TileMap
        get() = level.tileMap

    val pendingTransitionTileMap: TileMap
        get() {
            val setIdx = pendingSetIndex ?: currentSetIndex
            val levelIdx =
                pendingLevelIndex ?: currentLevelIndex
            return levelSets[setIdx].levels[levelIdx].tileMap
        }

    val levelName: String
        get() = level.name

    init {
        val sets = injectedSets ?: (loadLevelSets() ?: emptyList())
        rebuildState(sets)
    }

    fun selectSetById(setId: Int) {
        val setIdx = levelSets.indexOfFirst { it.id == setId }
        if (setIdx == -1) return

        pendingSetIndex = setIdx

        val levels = levelSets[setIdx].levels
        val firstIncompleteIndex = levels.indexOfFirst { !it.isCompleted }
        val levelIdx = if (firstIncompleteIndex != -1) firstIncompleteIndex else 0

        pendingLevelIndex = levelIdx
        uiModeState.longValue = UiMode.LEVEL_TRANSITION.ordinal.toLong()
    }

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
        // onRenderDelta?.invoke(currentLevelLoadedDelta())  // removed
    }

    fun selectLevel(name: String) {
        val index = levelsInCurrentSet.indexOfFirst { it.name == name }
        if (index != -1) {
            beginLevelTransition(index)
        }
    }

    fun restart() {
        markChanged()
        gameEngine = GameEngine(level)
        emitStateChanged(RenderDelta.StateChangeAnnotation.Restart)
    }

    private fun beginLevelTransition(nextIndex: Int) {
        pendingSetIndex = null
        pendingLevelIndex = nextIndex
        uiModeState.longValue = UiMode.LEVEL_TRANSITION.ordinal.toLong()
    }

    fun finishLevelTransition() {
        val setIdx = pendingSetIndex
        val levelIdx = pendingLevelIndex

        if (setIdx == null && levelIdx == null) return

        if (setIdx != null) {
            currentSetIndex = setIdx
        }

        val levels = levelsInCurrentSet
        val resolvedLevelIndex =
            levelIdx ?: currentLevelIndex.coerceIn(0, levels.lastIndex)

        currentLevelIndex = resolvedLevelIndex
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)

        pendingSetIndex = null
        pendingLevelIndex = null

        persistSelection()
        markChanged()
        // onRenderDelta?.invoke(currentLevelLoadedDelta())  // removed

        uiModeState.longValue = UiMode.GAMEPLAY.ordinal.toLong()
    }

    fun nextLevel() {
        val levels = levelsInCurrentSet
        val nextIndex = (currentLevelIndex + 1) % levels.size
        beginLevelTransition(nextIndex)
    }

    fun previousLevel() {
        val levels = levelsInCurrentSet
        val nextIndex =
            if (currentLevelIndex - 1 < 0) levels.size - 1 else currentLevelIndex - 1
        beginLevelTransition(nextIndex)
    }

    fun undo(): Boolean {
        if (gameEngine.undo() == null) return false
        markChanged()
        emitStateChanged(RenderDelta.StateChangeAnnotation.Undo)
        return true
    }

    fun movePlayerTo(position: Position) {
        val changed = gameEngine.movePlayerTo(position)
        if (changed) {
            emitStateChanged(RenderDelta.StateChangeAnnotation.PlayerMoved)
        }
    }

    fun moveBoxTo(
        boxFrom: Position,
        boxTo: Position,
    ) {
        if (tileMap.isVoid(boxTo)) {
            val removed = gameEngine.pushBoxIntoVoid(boxFrom, boxTo)
            if (!removed) {
                onRenderDelta?.invoke(RenderDelta.MoveRejected)
                return
            }
            emitStateChanged(
                RenderDelta.StateChangeAnnotation.BoxRemoved(boxTo),
            )
            recordCompletionIfWon()
            notifyIfWon()
            return
        }
        val boxPath = gameEngine.moveBoxTo(boxFrom, boxTo)
        if (boxPath == null) {
            onRenderDelta?.invoke(RenderDelta.MoveRejected)
            return
        }
        emitStateChanged(
            RenderDelta.StateChangeAnnotation.BoxMoved(boxPath),
        )
        recordCompletionIfWon()
        notifyIfWon()
    }

    private val levelsInCurrentSet: List<Level>
        get() = levelSets[currentSetIndex].levels

    fun emitLevelLoaded(staticFrame: StaticBoardFrame) {
        onRenderDelta?.invoke(
            RenderDelta.LevelLoaded(
                staticFrame = staticFrame,
                playerPosition = playerPosition,
                boxPositions = boxPositions,
            ),
        )
    }

    private fun emitStateChanged(annotation: RenderDelta.StateChangeAnnotation? = null) {
        onRenderDelta?.invoke(
            RenderDelta.StateChanged(
                playerPosition = gameEngine.playerPosition,
                boxPositions = gameEngine.boxPositions,
                annotation = annotation,
            ),
        )
    }

    private fun loadLevelSets(): List<LevelSet>? = repository.loadSets()

    private fun rebuildState(sets: List<LevelSet>) {
        val nonEmpty = sets.filter { it.levels.isNotEmpty() }
        levelSets = nonEmpty
        currentSetIndex = 0
        currentLevelIndex = 0
        if (levelSets.isEmpty()) return
        restoreLastSelection()
        gameEngine = GameEngine(level)
        persistSelection()
    }

    private fun restoreLastSelection() {
        level = levelsInCurrentSet[currentLevelIndex]
        val (savedSetId, savedPuzzleId) = lastSelectionStore.load()
        val setIdx = levelSets.indexOfFirst { it.id == savedSetId }
        if (setIdx != -1) {
            currentSetIndex = setIdx
            val levelIdx = levelsInCurrentSet.indexOfFirst { it.puzzleId == savedPuzzleId }
            if (levelIdx != -1) {
                currentLevelIndex = levelIdx
            }
        }
        level = levelsInCurrentSet[currentLevelIndex]
    }

    private fun persistSelection() {
        lastSelectionStore.save(levelSets[currentSetIndex].id, level.puzzleId)
    }

    private fun markChanged() {
        revisionState.longValue = revisionState.longValue + 1L
    }

    private fun recordCompletionIfWon() {
        if (gameEngine.isCleanWin) {
            val timestamp =
                repository.recordCompletion(
                    level,
                    gameEngine.getBoxMoveHistory(),
                )
            level.markCompleted(timestamp)
        }
    }

    private fun notifyIfWon() {
        if (gameEngine.isGameWon) {
            markChanged()
            onRenderDelta?.invoke(RenderDelta.GameWon(isClean = gameEngine.isCleanWin))
        }
    }

    private fun createRenderer(context: Context): GameRenderer =
        GameRenderer(
            assets = AndroidGameAssets(context),
            backgroundDrawer = BackgroundDrawer(context),
            tileDrawer = TileDrawer(),
            entityDrawer = EntityDrawer(AndroidGameAssets(context)),
        )

    internal fun buildStaticBoardFrame(
        context: Context,
        tileMap: TileMap,
        width: Int,
        height: Int,
    ): StaticBoardFrame {
        val renderer = createRenderer(context)

        val viewport =
            computeBoardViewport(
                surfaceWidth = width.toFloat(),
                surfaceHeight = height.toFloat(),
                innerRows = tileMap.rowCount,
                innerCols = tileMap.columnCount,
            )

        renderer.rebuildStaticLayout(
            viewWidth = width,
            viewHeight = height,
            viewport = viewport,
            tileMap = tileMap,
        )

        val bitmap = createBitmap(width, height)
        renderer.drawStaticFrame(Canvas(bitmap))

        return StaticBoardFrame(
            bitmap = bitmap,
            viewport = viewport,
            tileMap = tileMap,
            width = width,
            height = height,
        )
    }
}
