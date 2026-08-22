package com.example.einkarcade

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.einkarcade.appstate.LastSelectionStore
import com.example.einkarcade.appstate.SelectionStore
import com.example.einkarcade.catalog.LevelCatalog
import com.example.einkarcade.catalog.LevelSummary
import com.example.einkarcade.catalog.LevelSummaryMapper
import com.example.einkarcade.catalog.RepositoryLevelCatalog
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.data.LevelDataSource
import com.example.einkarcade.data.LevelsRepository
import com.example.einkarcade.session.GameSession
import com.example.einkarcade.session.LevelCompletionRecorder
import com.example.einkarcade.session.LevelMetadataService
import com.example.einkarcade.session.LevelNavigator
import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.GameRenderEvent
import com.example.einkarcade.ui.GameScreenState
import com.example.einkarcade.ui.GameUiMode
import com.example.einkarcade.ui.LevelTransitionSnapshot

class GameController(
    context: Context,
    injectedSets: List<LevelSet>? = null,
    private val selectionStore: SelectionStore = LastSelectionStore(context),
    levelCatalog: LevelCatalog = RepositoryLevelCatalog(context, injectedSets),
    private val dataSource: LevelDataSource = LevelsRepository(context),
) {
    private val levelMetadataService = LevelMetadataService(levelCatalog)
    private val levelCompletionRecorder = LevelCompletionRecorder(dataSource)
    private val gameScreenState = mutableStateOf<GameScreenState?>(null)
    private val uiModeState = mutableStateOf(GameUiMode.GAMEPLAY)
    private val transitionSnapshotState = mutableStateOf<LevelTransitionSnapshot?>(null)
    private lateinit var navigator: LevelNavigator
    private lateinit var session: GameSession

    val screenState: State<GameScreenState?>
        get() = gameScreenState

    val uiMode: GameUiMode
        get() = uiModeState.value

    val transitionSnapshot: State<LevelTransitionSnapshot?>
        get() = transitionSnapshotState

    val playerPosition: Position
        get() = requireSession().playerPosition

    val boxPositions: Set<Position>
        get() = requireSession().boxPositions

    val tileMap: TileMap
        get() = requireScreenState().tileMap

    val currentPuzzleId: Int
        get() = requireScreenState().puzzleId

    var onRenderEvent: ((GameRenderEvent) -> Unit)? = null

    init {
        rebuildState(injectedSets ?: dataSource.loadSets().orEmpty())
    }

    fun selectSetById(setId: Int) {
        beginLevelTransition { navigator.selectSet(setId) }
    }

    fun selectLevelByPuzzleId(puzzleId: Int) {
        beginLevelTransition { navigator.selectLevel(puzzleId) }
    }

    fun advanceToNextLevel() {
        beginLevelTransition { navigator.selectNextLevel() }
    }

    fun levels(): List<Level> = navigator.levelsInCurrentSet

    fun getCurrentLevelSummaries(): List<LevelSummary> = navigator.levelsInCurrentSet.map(LevelSummaryMapper::map)

    fun getCurrentRating(): Int = requireScreenState().rating

    fun toggleThumbUp() = toggleLikeByPuzzleId(currentPuzzleId)

    fun toggleThumbDown() = toggleDislikeByPuzzleId(currentPuzzleId)

    fun toggleStar() = toggleStarByPuzzleId(currentPuzzleId)

    fun toggleLikeByPuzzleId(puzzleId: Int) {
        val target = findCurrentSetLevel(puzzleId) ?: return
        val rating = levelMetadataService.toggleLike(target)
        if (puzzleId == currentPuzzleId) updateScreenState { it.copy(rating = rating) }
    }

    fun toggleDislikeByPuzzleId(puzzleId: Int) {
        val target = findCurrentSetLevel(puzzleId) ?: return
        val rating = levelMetadataService.toggleDislike(target)
        if (puzzleId == currentPuzzleId) updateScreenState { it.copy(rating = rating) }
    }

    fun toggleStarByPuzzleId(puzzleId: Int) {
        val target = findCurrentSetLevel(puzzleId) ?: return
        val starred = levelMetadataService.toggleStar(target)
        if (puzzleId == currentPuzzleId) updateScreenState { it.copy(isStarred = starred) }
    }

    fun reloadLevelSets() {
        rebuildState(dataSource.loadSets().orEmpty())
    }

    fun restart() {
        requireSession().restart()
        emitStateChanged()
        uiModeState.value = GameUiMode.GAMEPLAY
    }

    fun finishLevelTransition() {
        transitionSnapshotState.value = null
        uiModeState.value = GameUiMode.GAMEPLAY
    }

    fun undoLastMoveAt(position: Position): Boolean {
        if (uiMode != GameUiMode.GAMEPLAY || !requireSession().undoLastMoveAt(position)) return false
        emitStateChanged()
        return true
    }

    fun movePlayerTo(position: Position) {
        if (requireSession().movePlayerTo(position)) {
            emitStateChanged()
        }
    }

    fun moveBox(
        boxFrom: Position,
        boxTo: Position,
    ) {
        val session = requireSession()
        val annotation =
            when (val result = session.moveBox(boxFrom, boxTo)) {
                GameEngine.BoxMoveResult.Rejected -> {
                    emit(GameRenderEvent.MoveRejected)
                    return
                }

                is GameEngine.BoxMoveResult.Moved -> {
                    GameRenderEvent.StateChangeAnnotation.BoxMoved(
                        result.path,
                    )
                }

                is GameEngine.BoxMoveResult.Removed -> {
                    GameRenderEvent.StateChangeAnnotation.BoxRemoved(
                        result.position,
                    )
                }
            }

        emitStateChanged(annotation)
        when (levelCompletionRecorder.record(session)) {
            LevelCompletionRecorder.Result.NOT_SOLVED -> {
                Unit
            }

            LevelCompletionRecorder.Result.CLEAN_SOLUTION -> {
                uiModeState.value = GameUiMode.LEVEL_SOLVED
            }

            LevelCompletionRecorder.Result.CHEAT_SOLUTION -> {
                uiModeState.value = GameUiMode.LEVEL_SOLVED
                emit(GameRenderEvent.LevelSolvedWithCheat)
            }
        }
    }

    private fun beginLevelTransition(select: () -> Boolean) {
        if (!::navigator.isInitialized || !navigator.hasLevels) return
        val oldTileMap = tileMap
        if (!select()) return
        startSession()
        transitionSnapshotState.value = LevelTransitionSnapshot(oldTileMap)
        uiModeState.value = GameUiMode.LEVEL_TRANSITION
    }

    private fun rebuildState(sets: List<LevelSet>) {
        navigator = LevelNavigator(sets, selectionStore)
        transitionSnapshotState.value = null
        uiModeState.value = GameUiMode.GAMEPLAY
        if (!navigator.hasLevels) {
            gameScreenState.value = null
            return
        }
        startSession()
    }

    private fun startSession() {
        session = GameSession(navigator.currentLevel)
        refreshScreenState()
    }

    private fun refreshScreenState() {
        val level = navigator.currentLevel
        val set = navigator.currentSet
        gameScreenState.value =
            GameScreenState(
                setName = set.name,
                setId = set.id,
                levelName = level.name,
                puzzleId = level.puzzleId,
                rating = level.rating,
                isStarred = level.isStarred,
                tileMap = level.tileMap,
            )
    }

    private fun findCurrentSetLevel(puzzleId: Int): Level? = navigator.levelsInCurrentSet.firstOrNull { it.puzzleId == puzzleId }

    private fun emitStateChanged(annotation: GameRenderEvent.StateChangeAnnotation? = null) {
        val session = requireSession()
        emit(
            GameRenderEvent.StateChanged(
                playerPosition = session.playerPosition,
                boxPositions = session.boxPositions,
                annotation = annotation,
            ),
        )
    }

    private fun emit(event: GameRenderEvent) {
        onRenderEvent?.invoke(event)
    }

    private fun requireScreenState(): GameScreenState = requireNotNull(gameScreenState.value) { "Game screen state is not initialized" }

    private fun requireSession(): GameSession {
        check(::session.isInitialized) { "Game session is not initialized" }
        return session
    }

    private fun updateScreenState(transform: (GameScreenState) -> GameScreenState) {
        gameScreenState.value = transform(requireScreenState())
    }
}
