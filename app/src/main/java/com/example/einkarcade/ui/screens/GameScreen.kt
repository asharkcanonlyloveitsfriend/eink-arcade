@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.catalog.RepositoryLevelCatalog
import com.example.einkarcade.ui.GameHud
import com.example.einkarcade.ui.GameTitleBar
import com.example.einkarcade.ui.GameUiMode
import com.example.einkarcade.ui.SideControlsOverlay
import com.example.einkarcade.ui.modes.LevelPickerOverlay
import com.example.einkarcade.ui.modes.LevelSetPickerOverlay
import com.example.einkarcade.ui.modes.LevelSolvedOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
) {
    val screenState =
        requireNotNull(gameController.screenState.value) { "Game screen state is not initialized" }
    val uiMode = gameController.uiMode
    val context = androidx.compose.ui.platform.LocalContext.current
    val levelCatalog = remember(context) { RepositoryLevelCatalog(context = context) }
    val currentSetName = screenState.setName
    val currentLevelName = screenState.levelName
    var showLevelPicker by remember { mutableStateOf(false) }
    var showLevelSetPicker by remember { mutableStateOf(false) }
    var pickerRefreshNonce by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        gameController.undo()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        GameBoardHost(
            modifier = Modifier.fillMaxSize(),
            gameController = gameController,
        )

        if (uiMode == GameUiMode.LEVEL_SOLVED) {
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

            if (uiMode == GameUiMode.GAMEPLAY) {
                GameHud(
                    currentRating = screenState.rating,
                    onThumbUp = { gameController.toggleThumbUp() },
                    onThumbDown = { gameController.toggleThumbDown() },
                )
            }
        }

        if (uiMode != GameUiMode.LEVEL_TRANSITION) {
            SideControlsOverlay(
                showRestartButton = gameController.showRestartControl.value,
                onRestart = { gameController.restart() },
                onSkip = { gameController.nextLevel() },
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
