package com.example.einkarcade.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.GameBoardPresenter
import com.example.einkarcade.ui.rendering.GameBoardView

private fun createGameSurface(context: android.content.Context): GameBoardPresenter =
    GameBoardView(context)


@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController
) {
    gameController.revision.value
    val syncError = remember { mutableStateOf<String?>(null) }
    val syncSuccess = remember { mutableStateOf(false) }
    val surfaceRef = remember { mutableStateOf<GameBoardPresenter?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val surface = remember {
        createGameSurface(context)
    }
    if (surfaceRef.value == null) {
        surfaceRef.value = surface
    }
    val currentSetName = gameController.currentSetName
    val currentLevelName = gameController.levelName

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

    BackHandler(enabled = true) {
        GameInputHandler.handleBackKeyUp(
            gameController = gameController
        )
    }

    @Composable
    fun BottomIconButton(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String,
        backgroundColor: Color = Color.Transparent,
        pressedBackgroundColor: Color = Color.Transparent,
        tintColor: Color = Color.LightGray,
        pressedTintAlpha: Float = 0.6f
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()
        val currentTint = if (isPressed.value) {
            tintColor.copy(alpha = tintColor.alpha * pressedTintAlpha)
        } else {
            tintColor
        }

        Box(
            modifier = Modifier
                .height(48.dp)
                .background(if (isPressed.value) pressedBackgroundColor else backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = currentTint
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gameCanvas"),
            factory = {
                val selection = object : GameInputHandler.BoxSelection {
                    override fun getSelectedBox(): Position? =
                        surface.getSelectedBox()

                    override fun setSelectedBox(position: Position?) {
                        surface.setSelectedBox(position)
                    }
                }

                surface.setOnTapCell { pos ->
                    GameInputHandler.handleTap(
                        tappedPosition = pos,
                        gameController = gameController,
                        selection = selection
                    )
                }

                surface.asView()
            }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- Set (top-left) ---
                val setExpanded = remember { mutableStateOf(false) }
                val setOptions = gameController.availableSetOptions

                Box(
                    modifier = Modifier
                        .clickable { setExpanded.value = true }
                ) {
                    Text(
                        text = currentSetName,
                        fontSize = 16.sp,
                        color = Color.LightGray,
                        modifier = Modifier
                            .background(
                                Color.Black,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    DropdownMenu(
                        expanded = setExpanded.value,
                        onDismissRequest = { setExpanded.value = false }
                    ) {
                        Column(
                            modifier = Modifier.heightIn(max = 800.dp)
                        ) {
                            setOptions.forEach { (id, name) ->
                                val isSelected = name == currentSetName
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        gameController.selectSetById(id)
                                        setExpanded.value = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) Color.LightGray else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- Level (top-right, right-aligned) ---
                val levelExpanded = remember { mutableStateOf(false) }
                val levels = gameController.levels()
                val selectedLevelIndex = levels.indexOfFirst { it.name == currentLevelName }
                val levelScrollState = rememberScrollState()
                val density = LocalDensity.current
                val itemHeight: Dp = 40.dp

                Box(
                    modifier = Modifier
                        .clickable { levelExpanded.value = true }
                ) {
                    Text(
                        text = currentLevelName,
                        fontSize = 16.sp,
                        color = Color.LightGray,
                        modifier = Modifier
                            .background(
                                Color.Black,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    DropdownMenu(
                        expanded = levelExpanded.value,
                        onDismissRequest = { levelExpanded.value = false }
                    ) {
                        LaunchedEffect(levelExpanded.value, selectedLevelIndex) {
                            if (levelExpanded.value && selectedLevelIndex >= 0) {
                                val targetIndex = (selectedLevelIndex - 2).coerceAtLeast(0)
                                val targetOffset =
                                    with(density) { (itemHeight * targetIndex).roundToPx() }
                                levelScrollState.scrollTo(targetOffset)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .heightIn(max = 800.dp)
                                .verticalScroll(levelScrollState)
                        ) {
                            levels.forEach { lvl ->
                                val completedMark = if (lvl.isCompleted) " ✓" else ""
                                val ratingBadge =
                                    when (lvl.rating) { 1 -> " 👍"; -1 -> " 👎"; else -> "" }
                                val isSelected = lvl.name == currentLevelName

                                DropdownMenuItem(
                                    text = { Text(lvl.name + completedMark + ratingBadge) },
                                    onClick = {
                                        gameController.selectLevel(lvl.name)
                                        levelExpanded.value = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) Color.LightGray else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentRating = gameController.getCurrentRating()

                BottomIconButton(
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
                    icon = when {
                        syncSuccess.value -> Icons.Filled.Check
                        syncError.value != null -> Icons.Filled.Warning
                        else -> Icons.Filled.Refresh
                    },
                    contentDescription = "Sync"
                )

                Spacer(modifier = Modifier.weight(1f))

                Spacer(
                    modifier = Modifier
                        .height(48.dp)
                        .width(144.dp)
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { gameController.nextLevel() }
                        )
                )

                BottomIconButton(
                    onClick = {
                        syncSuccess.value = false
                        syncError.value = null
                        gameController.toggleThumbDown()
                    },
                    icon = if (currentRating == -1) Icons.Filled.Delete else Icons.Outlined.Delete,
                    contentDescription = "Dislike level"
                )

                BottomIconButton(
                    onClick = {
                        syncSuccess.value = false
                        syncError.value = null
                        gameController.toggleThumbUp()
                    },
                    icon = if (currentRating == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like level"
                )

            }
        }

        if (gameController.isGameWon) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        gameController.nextLevel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.White)
                        .border(width = 2.dp, color = Color.Black)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentRating = gameController.getCurrentRating()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BottomIconButton(
                                onClick = { gameController.toggleThumbDown() },
                                icon = if (currentRating == -1) Icons.Filled.Delete else Icons.Outlined.Delete,
                                contentDescription = "Dislike level",
                                backgroundColor = Color.White,
                                pressedBackgroundColor = Color.White,
                                tintColor = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "You win!",
                                color = Color.Black,
                                fontSize = 32.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            BottomIconButton(
                                onClick = { gameController.toggleThumbUp() },
                                icon = if (currentRating == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like level",
                                backgroundColor = Color.White,
                                pressedBackgroundColor = Color.White,
                                tintColor = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
