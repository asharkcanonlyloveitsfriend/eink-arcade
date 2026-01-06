package com.example.einkarcade.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.einkarcade.GameController
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.GameSurfaceView


@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController
) {
    gameController.revision.value
    val syncError = remember { mutableStateOf<String?>(null) }
    val syncSuccess = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val surfaceViewRef = remember { mutableStateOf<GameSurfaceView?>(null) }
    val currentGameController = rememberUpdatedState(gameController)
    val currentSetName = gameController.currentSetName
    val currentLevelName = gameController.levelName

    DisposableEffect(surfaceViewRef.value) {
        val surfaceView = surfaceViewRef.value
        val sink: (GameController.RenderDelta) -> Unit = { delta ->
            surfaceView?.applyDelta(delta)
        }
        gameController.onRenderDelta = sink
        onDispose {
            if (gameController.onRenderDelta === sink) {
                gameController.onRenderDelta = null
            }
        }
    }


    BackHandler(enabled = true) {
        // handled manually via key events below
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    @Composable
    fun BottomIconButton(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String,
        backgroundColor: Color = Color.Black,
        pressedBackgroundColor: Color = Color.DarkGray,
        tintColor: Color = Color.LightGray
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed = interactionSource.collectIsPressedAsState()

        Box(
            modifier = Modifier
                .height(48.dp)
                .background(if (isPressed.value) pressedBackgroundColor else backgroundColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp)
                .focusProperties { canFocus = false },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Back) {
                    when (event.type) {
                        KeyEventType.KeyDown -> true
                        KeyEventType.KeyUp -> {
                            GameInputHandler.handleBackKeyUp(
                                gameController = gameController
                            )
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gameCanvas"),
            factory = { context ->
                val view = GameSurfaceView(context)
                val selection = object : GameInputHandler.BoxSelection {
                    override fun getSelectedBox(): Position? = view.getSelectedBox()

                    override fun setSelectedBox(position: Position?) {
                        view.setSelectedBox(position)
                    }
                }
                view.setOnTapCell { pos ->
                    GameInputHandler.handleTap(
                        tappedPosition = pos,
                        gameController = currentGameController.value,
                        selection = selection
                    )
                }
                surfaceViewRef.value = view
                view
            },
            update = { view ->
                if (surfaceViewRef.value !== view) {
                    surfaceViewRef.value = view
                }
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
                            } catch (t: Throwable) {
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

                run {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed = interactionSource.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(144.dp)
                            .background(if (isPressed.value) Color.DarkGray else Color.Black)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { gameController.nextLevel() }
                            )
                            .focusProperties { canFocus = false },
                        contentAlignment = Alignment.Center
                    ) {}
                }

                // --- X (dislike) ---
                BottomIconButton(
                    onClick = {
                        syncSuccess.value = false
                        syncError.value = null
                        gameController.toggleThumbDown()
                    },
                    icon = if (currentRating == -1) Icons.Filled.Delete else Icons.Outlined.Delete,
                    contentDescription = "Dislike level"
                )

                // --- Heart (like) ---
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
