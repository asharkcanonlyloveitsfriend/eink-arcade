@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.screens

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.GameRenderEvent
import com.example.einkarcade.ui.GameUiMode
import com.example.einkarcade.ui.modes.LevelTransitionView
import com.example.einkarcade.ui.rendering.GameBoardView
import com.example.einkarcade.ui.rendering.gameBoardBottomReservedPx
import com.example.einkarcade.ui.rendering.gameBoardTopReservedPx
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport

private data class LoadedBoardKey(
    val puzzleId: Int,
    val boardSize: IntSize,
)

@Composable
internal fun GameBoardHost(
    gameController: GameController,
    modifier: Modifier = Modifier,
) {
    val screenState =
        requireNotNull(gameController.screenState.value) { "Game screen state is not initialized" }
    val uiMode = gameController.uiMode
    val transitionSnapshot = gameController.transitionSnapshot.value
    val currentTileMap = screenState.tileMap
    val currentPuzzleId = screenState.puzzleId
    val context = LocalContext.current
    val surface = remember(context) { GameBoardView(context) }
    val stationaryTapTracker =
        remember { StationaryTapTracker(ViewConfiguration.getDoubleTapTimeout().toLong()) }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val loadedBoardKey =
        remember(gameController, surface) { mutableStateOf<LoadedBoardKey?>(null) }

    DisposableEffect(gameController, surface) {
        val renderHandler: (GameRenderEvent) -> Unit = surface::applyEvent
        val tapHandler: (Position, Long) -> Unit = { position, eventTimeMillis ->
            val isDoubleTap = stationaryTapTracker.consumeMatchingTap(position, eventTimeMillis)
            val wasHandledAsGesture =
                when {
                    isDoubleTap && position == gameController.playerPosition -> {
                        gameController.restart()
                        true
                    }

                    isDoubleTap -> gameController.undoLastMoveAt(position)
                    else -> false
                }
            if (!wasHandledAsGesture) {
                val playerPositionBeforeTap = gameController.playerPosition
                val boxPositionsBeforeTap = gameController.boxPositions
                surface.selectedBox =
                    GameInputHandler.handleTap(
                        tappedPosition = position,
                        gameController = gameController,
                        selectedBox = surface.selectedBox,
                    )
                stationaryTapTracker.recordTap(
                    tappedPosition = position,
                    eventTimeMillis = eventTimeMillis,
                    causedEntityMove =
                        gameController.playerPosition != playerPositionBeforeTap ||
                            gameController.boxPositions != boxPositionsBeforeTap,
                )
            }
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
        if (uiMode == GameUiMode.GAMEPLAY &&
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
        if (uiMode == GameUiMode.LEVEL_TRANSITION) {
            loadedBoardKey.value = null
            stationaryTapTracker.clear()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("gameCanvas"),
            factory = { surface },
        )

        if (uiMode == GameUiMode.LEVEL_TRANSITION) {
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
                            minimumTopMarginPx = context.gameBoardTopReservedPx(),
                            minimumBottomMarginPx = context.gameBoardBottomReservedPx(),
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
    }
}
