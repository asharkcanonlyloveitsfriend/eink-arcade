@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.catalog.RepositoryLevelCatalog
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.modes.LevelPickerOverlay
import com.example.einkarcade.ui.modes.LevelSetPickerOverlay
import com.example.einkarcade.ui.modes.LevelSolvedOverlay
import com.example.einkarcade.ui.modes.LevelTransitionView
import com.example.einkarcade.ui.rendering.GameBoardPresenter
import com.example.einkarcade.ui.rendering.GameBoardView
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport

private fun createGameSurface(context: android.content.Context): GameBoardPresenter = GameBoardView(context)

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
) {
    gameController.revision.value
    val uiMode = gameController.uiMode
    val currentTileMap: TileMap = gameController.tileMap
    val syncError = remember { mutableStateOf<String?>(null) }
    val syncSuccess = remember { mutableStateOf(false) }
    val surfaceRef = remember { mutableStateOf<GameBoardPresenter?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val surface =
        remember {
            createGameSurface(context)
        }
    if (surfaceRef.value == null) {
        surfaceRef.value = surface
    }
    val levelCatalog = remember(context) { RepositoryLevelCatalog(context = context) }
    val currentSetName = gameController.currentSetName
    val currentLevelName = gameController.levelName
    val boardWidth = remember { mutableIntStateOf(0) }
    val boardHeight = remember { mutableIntStateOf(0) }
    val hasEmittedLevelLoaded = remember { mutableStateOf(false) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var showLevelSetPicker by remember { mutableStateOf(false) }

    DisposableEffect(surfaceRef.value) {
        val surface = surfaceRef.value
        val sink: (GameController.RenderDelta) -> Unit = { delta ->
            surface?.applyDelta(delta)
        }
        gameController.onRenderDelta = sink
        onDispose {
            if (gameController.onRenderDelta === sink) {
                gameController.onRenderDelta = null
            }
        }
    }

    DisposableEffect(surfaceRef.value) {
        val surface = surfaceRef.value
        if (surface is GameBoardView) {
            val view = surface.asView()
            view.addOnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
                val width = right
                val height = bottom
                if (width > 0 && height > 0) {
                    boardWidth.intValue = width
                    boardHeight.intValue = height
                }
            }
        }
        onDispose { }
    }

    LaunchedEffect(boardWidth.value, boardHeight.value, uiMode) {
        if (uiMode == GameController.UiMode.GAMEPLAY &&
            boardWidth.value > 0 &&
            boardHeight.value > 0 &&
            !hasEmittedLevelLoaded.value
        ) {
            val frame =
                gameController.buildStaticBoardFrame(
                    context = context,
                    tileMap = currentTileMap,
                    width = boardWidth.value,
                    height = boardHeight.value,
                )

            gameController.emitLevelLoaded(frame)
            hasEmittedLevelLoaded.value = true
        }
    }

    LaunchedEffect(uiMode) {
        if (uiMode == GameController.UiMode.LEVEL_TRANSITION) {
            hasEmittedLevelLoaded.value = false
        }
    }

    BackHandler(enabled = true) {
        GameInputHandler.handleBackKeyUp(
            gameController = gameController,
        )
    }

    @Composable
    fun bottomIconButton(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String,
        backgroundColor: Color = Color.Transparent,
        pressedBackgroundColor: Color = Color.Transparent,
        tintColor: Color = Color.LightGray,
        pressedTintAlpha: Float = 0.6f,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()
        val currentTint =
            if (isPressed.value) {
                tintColor.copy(alpha = tintColor.alpha * pressedTintAlpha)
            } else {
                tintColor
            }

        Box(
            modifier =
                Modifier
                    .height(48.dp)
                    .background(if (isPressed.value) pressedBackgroundColor else backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = currentTint,
            )
        }
    }

    @Composable
    fun bottomDrawableButton(
        onClick: () -> Unit,
        drawableResId: Int,
        contentDescription: String,
        backgroundColor: Color = Color.Transparent,
        pressedBackgroundColor: Color = Color.Transparent,
        tintColor: Color = Color.LightGray,
        pressedTintAlpha: Float = 0.6f,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()
        val currentTint =
            if (isPressed.value) {
                tintColor.copy(alpha = tintColor.alpha * pressedTintAlpha)
            } else {
                tintColor
            }
        Box(
            modifier =
                Modifier
                    .height(48.dp)
                    .background(if (isPressed.value) pressedBackgroundColor else backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    ).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter =
                    androidx.compose.ui.res
                        .painterResource(drawableResId),
                contentDescription = contentDescription,
                tint = currentTint,
            )
        }
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
            factory = {
                val selection =
                    object : GameInputHandler.BoxSelection {
                        override fun getSelectedBox(): Position? = surface.getSelectedBox()

                        override fun setSelectedBox(position: Position?) {
                            surface.setSelectedBox(position)
                        }
                    }

                surface.setOnTapCell { pos ->
                    GameInputHandler.handleTap(
                        tappedPosition = pos,
                        gameController = gameController,
                        selection = selection,
                    )
                }

                surface.asView()
            },
        )

        if (uiMode == GameController.UiMode.LEVEL_TRANSITION) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val width = boardWidth.value
                    val height = boardHeight.value

                    check(width > 0 && height > 0) {
                        "LevelTransitionView requires board size before construction"
                    }
                    val oldViewport =
                        computeBoardViewport(
                            surfaceWidth = width.toFloat(),
                            surfaceHeight = height.toFloat(),
                            innerRows = currentTileMap.rowCount,
                            innerCols = currentTileMap.columnCount,
                        )

                    val newFrame =
                        gameController.buildStaticBoardFrame(
                            context = ctx,
                            tileMap = gameController.pendingTransitionTileMap,
                            width = width,
                            height = height,
                        )

                    LevelTransitionView(ctx).apply {
                        setTransitionData(
                            oldViewport = oldViewport,
                            oldTileMap = currentTileMap,
                            newFrame = newFrame,
                        )
                        onDismiss = {
                            gameController.finishLevelTransition()
                            gameController.emitLevelLoaded(newFrame)
                            hasEmittedLevelLoaded.value = true
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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // --- Set (top-left) ---
                Text(
                    text = currentSetName,
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    modifier =
                        Modifier
                            .clickable { showLevelSetPicker = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                // --- Level (top-right, right-aligned) ---
                Text(
                    text = currentLevelName,
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    modifier =
                        Modifier
                            .clickable { showLevelPicker = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val currentRating = gameController.getCurrentRating()

                bottomIconButton(
                    onClick = {
                        syncError.value = null
                        syncSuccess.value = false
                        val handler = Handler(Looper.getMainLooper())
                        Thread {
                            try {
                                gameController.syncWithServer()
                                handler.post {
                                    syncSuccess.value = true
                                }
                            } catch (_: Throwable) {
                                handler.post {
                                    syncError.value = "Sync failed."
                                    syncSuccess.value = false
                                }
                            }
                        }.start()
                    },
                    icon =
                        when {
                            syncSuccess.value -> Icons.Filled.Check
                            syncError.value != null -> Icons.Filled.Warning
                            else -> Icons.Filled.Refresh
                        },
                    contentDescription = "Sync",
                )

                Spacer(modifier = Modifier.weight(1f))

                Spacer(
                    modifier =
                        Modifier
                            .height(48.dp)
                            .width(144.dp)
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { gameController.nextLevel() },
                            ),
                )

                bottomDrawableButton(
                    onClick = {
                        syncSuccess.value = false
                        syncError.value = null
                        gameController.toggleThumbDown()
                    },
                    drawableResId =
                        if (currentRating ==
                            -1
                        ) {
                            com.example.einkarcade.R.drawable.ic_trash_filled
                        } else {
                            com.example.einkarcade.R.drawable.ic_trash
                        },
                    contentDescription = "Dislike level",
                )

                bottomDrawableButton(
                    onClick = {
                        syncSuccess.value = false
                        syncError.value = null
                        gameController.toggleThumbUp()
                    },
                    drawableResId =
                        if (currentRating ==
                            1
                        ) {
                            com.example.einkarcade.R.drawable.ic_heart_filled
                        } else {
                            com.example.einkarcade.R.drawable.ic_heart
                        },
                    contentDescription = "Like level",
                )
            }
        }

        if (showLevelPicker) {
            LevelPickerOverlay(
                catalog = levelCatalog,
                selectedSetId = gameController.currentSetId,
                selectedPuzzleId = gameController.currentPuzzleId,
                onPickLevel = { puzzleId -> gameController.selectLevelByPuzzleId(puzzleId) },
                onToggleLike = { puzzleId -> gameController.toggleLikeByPuzzleId(puzzleId) },
                onToggleDislike = { puzzleId -> gameController.toggleDislikeByPuzzleId(puzzleId) },
                onDismiss = { showLevelPicker = false },
            )
        }
        if (showLevelSetPicker) {
            LevelSetPickerOverlay(
                catalog = levelCatalog,
                selectedSetId = gameController.currentSetId,
                onPickSet = { setId -> gameController.selectSetById(setId) },
                onDismiss = { showLevelSetPicker = false },
            )
        }
    }
}
