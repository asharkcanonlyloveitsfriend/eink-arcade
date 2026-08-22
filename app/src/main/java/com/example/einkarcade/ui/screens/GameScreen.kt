@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.catalog.RepositoryLevelCatalog
import com.example.einkarcade.data.LevelSetService
import com.example.einkarcade.ui.GameHud
import com.example.einkarcade.ui.GameTitleBar
import com.example.einkarcade.ui.GameUiMode
import com.example.einkarcade.ui.modes.LevelPickerOverlay
import com.example.einkarcade.ui.modes.LevelSetPickerOverlay
import com.example.einkarcade.ui.modes.LevelSolvedOverlay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
) {
    val screenState = gameController.screenState.value
    val uiMode = gameController.uiMode
    val context = androidx.compose.ui.platform.LocalContext.current
    val levelCatalog = remember(context) { RepositoryLevelCatalog(context = context) }
    val levelSetService = remember(context) { LevelSetService(context) }
    var showLevelPicker by remember { mutableStateOf(false) }
    var showLevelSetPicker by remember { mutableStateOf(screenState == null) }
    var pickerRefreshNonce by remember { mutableLongStateOf(0L) }
    var levelSetError by remember { mutableStateOf<LevelSetError?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        levelSetService.import(uri)
                        gameController.reloadLevelSets()
                        pickerRefreshNonce++
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        levelSetError =
                            LevelSetError(
                                title = "Level set couldn't be imported",
                                message = exception.message ?: "The selected file could not be read.",
                            )
                    }
                }
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        screenState?.let { activeScreenState ->
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
                            onAdvance = { gameController.advanceToNextLevel() }
                        }
                    },
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                GameTitleBar(
                    setName = activeScreenState.setName,
                    levelName = activeScreenState.levelName,
                    onOpenSetPicker = { showLevelSetPicker = true },
                    onOpenLevelPicker = { showLevelPicker = true },
                    isStarred = activeScreenState.isStarred,
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
                        currentRating = activeScreenState.rating,
                        onThumbUp = { gameController.toggleThumbUp() },
                        onThumbDown = { gameController.toggleThumbDown() },
                    )
                }
            }

            if (showLevelPicker) {
                LevelPickerOverlay(
                    levels = gameController.getCurrentLevelSummaries(),
                    selectedPuzzleId = activeScreenState.puzzleId,
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
        }

        if (showLevelSetPicker || screenState == null) {
            LevelSetPickerOverlay(
                catalog = levelCatalog,
                selectedSetId = screenState?.setId,
                onPickSet = { setId -> gameController.selectSetById(setId) },
                onImport = {
                    importLauncher.launch(
                        arrayOf(
                            "text/plain",
                            "application/octet-stream",
                            "application/xml",
                            "text/xml",
                            "application/x-sokoban",
                            "application/x-slc",
                        ),
                    )
                },
                onRename = { setId, title ->
                    coroutineScope.launch {
                        try {
                            levelSetService.rename(setId, title)
                            gameController.reloadLevelSets()
                            pickerRefreshNonce++
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            levelSetError =
                                LevelSetError(
                                    title = "Level set couldn't be renamed",
                                    message = exception.message ?: "The level set could not be renamed.",
                                )
                        }
                    }
                },
                onDelete = { setId ->
                    coroutineScope.launch {
                        try {
                            levelSetService.delete(setId)
                            gameController.reloadLevelSets()
                            pickerRefreshNonce++
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            levelSetError =
                                LevelSetError(
                                    title = "Level set couldn't be deleted",
                                    message = exception.message ?: "The level set could not be deleted.",
                                )
                        }
                    }
                },
                refreshNonce = pickerRefreshNonce,
                errorTitle = levelSetError?.title,
                errorMessage = levelSetError?.message,
                onDismissError = { levelSetError = null },
                onDismiss = {
                    if (screenState != null) {
                        showLevelSetPicker = false
                    }
                },
            )
        }
    }
}

private data class LevelSetError(
    val title: String,
    val message: String,
)
