@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.catalog.RepositoryLevelCatalog
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.GameHud
import com.example.einkarcade.ui.GameTitleBar
import com.example.einkarcade.ui.SideControlsOverlay
import com.example.einkarcade.ui.modes.LevelPickerOverlay
import com.example.einkarcade.ui.modes.LevelSetPickerOverlay
import com.example.einkarcade.ui.modes.LevelSolvedOverlay
import com.example.einkarcade.ui.modes.LevelTransitionView
import com.example.einkarcade.ui.rendering.GameBoardView
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LoadedBoardKey(
    val puzzleId: Int,
    val boardSize: IntSize,
)

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
) {
    val screenState =
        requireNotNull(gameController.screenState.value) { "Game screen state is not initialized" }
    val uiMode = gameController.uiMode
    val transitionSnapshot = gameController.transitionSnapshot.value
    val currentTileMap = screenState.tileMap
    val context = androidx.compose.ui.platform.LocalContext.current
    val surface = remember(context) { GameBoardView(context) }
    val levelCatalog = remember(context) { RepositoryLevelCatalog(context = context) }
    val currentSetName = screenState.setName
    val currentLevelName = screenState.levelName
    val currentPuzzleId = screenState.puzzleId
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val loadedBoardKey =
        remember(gameController, surface) { mutableStateOf<LoadedBoardKey?>(null) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var showLevelSetPicker by remember { mutableStateOf(false) }
    var pickerRefreshNonce by remember { mutableLongStateOf(0L) }

    DisposableEffect(gameController, surface) {
        val renderHandler: (GameController.RenderEvent) -> Unit = surface::applyEvent
        val tapHandler: (Position) -> Unit = { position ->
            surface.selectedBox =
                GameInputHandler.handleTap(
                    tappedPosition = position,
                    gameController = gameController,
                    selectedBox = surface.selectedBox,
                )
        }
        gameController.onRenderEvent = renderHandler
        surface.setOnTapCell(tapHandler)
        onDispose {
            if (gameController.onRenderEvent === renderHandler) {
                gameController.onRenderEvent = null
            }
            surface.setOnTapCell(null)
        }
    }

    DisposableEffect(surface) {
        val listener =
            android.view.View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val width = right - left
                val height = bottom - top
                if (width > 0 && height > 0) {
                    boardSize = IntSize(width, height)
                }
            }
        surface.addOnLayoutChangeListener(listener)
        onDispose {
            surface.removeOnLayoutChangeListener(listener)
        }
    }

    LaunchedEffect(boardSize, uiMode, currentPuzzleId, gameController, surface) {
        val boardKey = LoadedBoardKey(currentPuzzleId, boardSize)
        if (uiMode == GameController.UiMode.GAMEPLAY &&
            boardSize != IntSize.Zero &&
            loadedBoardKey.value != boardKey
        ) {
            surface.loadLevel(
                tileMap = currentTileMap,
                playerPosition = gameController.playerPosition,
                boxPositions = gameController.boxPositions,
            )
            loadedBoardKey.value = boardKey
        }
    }

    LaunchedEffect(uiMode) {
        if (uiMode == GameController.UiMode.LEVEL_TRANSITION) {
            loadedBoardKey.value = null
        }
    }

    BackHandler(enabled = true) {
        gameController.undo()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("gameCanvas"),
            factory = { surface },
        )

        if (uiMode == GameController.UiMode.LEVEL_TRANSITION) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val snapshot =
                        requireNotNull(transitionSnapshot) { "Missing level transition snapshot" }
                    val width = boardSize.width
                    val height = boardSize.height

                    check(width > 0 && height > 0) {
                        "LevelTransitionView requires board size before construction"
                    }
                    val oldViewport =
                        computeBoardViewport(
                            surfaceWidth = width.toFloat(),
                            surfaceHeight = height.toFloat(),
                            innerRows = snapshot.oldTileMap.rowCount,
                            innerCols = snapshot.oldTileMap.columnCount,
                        )

                    val newFrame = surface.buildStaticBoardFrame(currentTileMap)

                    LevelTransitionView(ctx).apply {
                        setTransitionData(
                            oldViewport = oldViewport,
                            oldTileMap = snapshot.oldTileMap,
                            newFrame = newFrame,
                        )
                        onDismiss = {
                            gameController.finishLevelTransition()
                            surface.loadLevel(
                                staticFrame = newFrame,
                                playerPosition = gameController.playerPosition,
                                boxPositions = gameController.boxPositions,
                            )
                            loadedBoardKey.value = LoadedBoardKey(currentPuzzleId, boardSize)
                        }
                    }
                },
            )
        }

        if (uiMode == GameController.UiMode.LEVEL_SOLVED) {
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("levelSolvedView"),
                factory = { ctx ->
                    LevelSolvedOverlay(ctx).apply {
                        setRating(gameController.getCurrentRating())
                        onThumbUp = {
                            gameController.toggleThumbUp()
                            setRating(gameController.getCurrentRating())
                        }
                        onThumbDown = {
                            gameController.toggleThumbDown()
                            setRating(gameController.getCurrentRating())
                        }
                        onAdvance = { gameController.nextLevel() }
                    }
                },
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            GameTitleBar(
                setName = currentSetName,
                levelName = currentLevelName,
                onOpenSetPicker = { showLevelSetPicker = true },
                onOpenLevelPicker = { showLevelPicker = true },
                isStarred = screenState.isStarred,
                onToggleStar = { gameController.toggleStar() },
            )

            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            )

            if (uiMode == GameController.UiMode.GAMEPLAY) {
                GameHud(
                    currentRating = screenState.rating,
                    onThumbUp = { gameController.toggleThumbUp() },
                    onThumbDown = { gameController.toggleThumbDown() },
                )
            }
        }

        if (uiMode != GameController.UiMode.LEVEL_TRANSITION) {
            SideControlsOverlay(
                showRestartButton = gameController.showRestartControl.value,
                onRestart = { gameController.restart() },
                onSkip = { gameController.skipLevel() },
            )
        }

        if (showLevelPicker) {
            LevelPickerOverlay(
                levels = gameController.getCurrentLevelSummaries(),
                selectedPuzzleId = screenState.puzzleId,
                onPickLevel = { puzzleId -> gameController.selectLevelByPuzzleId(puzzleId) },
                onToggleLike = { puzzleId ->
                    gameController.toggleLikeByPuzzleId(puzzleId)
                    pickerRefreshNonce++
                },
                onToggleStar = { puzzleId ->
                    gameController.toggleStarByPuzzleId(puzzleId)
                    pickerRefreshNonce++
                },
                onToggleDislike = { puzzleId ->
                    gameController.toggleDislikeByPuzzleId(puzzleId)
                    pickerRefreshNonce++
                },
                refreshNonce = pickerRefreshNonce,
                onDismiss = { showLevelPicker = false },
            )
        }
        if (showLevelSetPicker) {
            LevelSetPickerOverlay(
                catalog = levelCatalog,
                selectedSetId = screenState.setId,
                onPickSet = { setId -> gameController.selectSetById(setId) },
                onRefresh = {
                    try {
                        withContext(Dispatchers.IO) {
                            gameController.syncWithServer()
                        }
                        true
                    } catch (_: Throwable) {
                        false
                    }
                },
                onDismiss = { showLevelSetPicker = false },
            )
        }
    }
}
