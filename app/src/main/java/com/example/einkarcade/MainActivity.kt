package com.example.einkarcade

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.einkarcade.ui.theme.EinkArcadeTheme

enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class Tile { EMPTY, WALL, BOX, TARGET, BOX_ON_TARGET }

data class Level(
    val grid: List<List<Tile>>,
    val playerStart: Position
) {
    companion object {
        fun fromAscii(ascii: String): Level {
            val lines = ascii.lines().dropLastWhile { it.isBlank() }
            val maxWidth = lines.maxOfOrNull { it.length } ?: 0
            var playerStart: Position? = null
            val grid = lines.mapIndexed { rowIndex, line ->
                line.padEnd(maxWidth).mapIndexed { colIndex, char ->
                    when (char) {
                        '#' -> Tile.WALL
                        '.' -> Tile.TARGET
                        '$' -> Tile.BOX
                        '@' -> {
                            playerStart = Position(rowIndex, colIndex)
                            Tile.EMPTY
                        }

                        ' ' -> Tile.EMPTY
                        else -> Tile.EMPTY
                    }
                }
            }
            requireNotNull(playerStart) { "Player start '@' not found in level" }
            return Level(grid, playerStart!!)
        }
    }
}

data class Position(val row: Int, val col: Int) {
    fun move(direction: Direction): Position = when (direction) {
        Direction.UP -> Position(row - 1, col)
        Direction.DOWN -> Position(row + 1, col)
        Direction.LEFT -> Position(row, col - 1)
        Direction.RIGHT -> Position(row, col + 1)
    }
}

data class GameUiState(val playerPosition: Position, val levelNumber: Int)

data class GameState(
    val grid: List<MutableList<Tile>>
) {
    fun isInBounds(position: Position): Boolean {
        return position.row in grid.indices && position.col in grid[0].indices
    }

    fun tileAt(position: Position): Tile? {
        return if (isInBounds(position)) grid[position.row][position.col] else null
    }

    companion object {
        fun fromLevel(level: Level): GameState {
            return GameState(
                grid = level.grid.map { it.toMutableList() }
            )
        }
    }
}

class GameController(context: Context, testLevels: List<String>? = null) {
    private val levels: List<String>
    private var currentLevelIndex = 0
    private var level: Level
    private var gameEngine: GameEngine

    init {
        levels = testLevels ?: run {
            val levelFiles = context.assets.list("levels")?.toList() ?: emptyList()
            levelFiles.map { filename ->
                context.assets.open("levels/$filename").bufferedReader().use { it.readText() }
            }
        }
        level = Level.fromAscii(levels[currentLevelIndex])
        gameEngine = GameEngine(level)
    }

    val currentLevel: Int
        get() = currentLevelIndex + 1

    val playerPosition: Position
        get() = gameEngine.playerPosition

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val gridHeight: Int
        get() = level.grid.size
    val gridWidth: Int
        get() = level.grid[0].size

    fun getTileAt(position: Position): Tile? {
        return gameEngine.getTileAt(position)
    }

    fun restart() {
        if (isGameWon) {
            nextLevel()
        } else {
            gameEngine = GameEngine(level)
        }
    }

    fun handleDirectionInput(direction: Direction) {
        gameEngine.move(direction)
    }

    fun nextLevel() {
        currentLevelIndex = (currentLevelIndex + 1) % levels.size
        level = Level.fromAscii(levels[currentLevelIndex])
        gameEngine = GameEngine(level)
    }

    fun previousLevel() {
        currentLevelIndex =
            if (currentLevelIndex - 1 < 0) levels.size - 1 else currentLevelIndex - 1
        level = Level.fromAscii(levels[currentLevelIndex])
        gameEngine = GameEngine(level)
    }

    fun undo() {
        gameEngine.undo()
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        internal const val CELL_SIZE = 100f
        internal const val GRID_OFFSET_X = 50f
        internal const val GRID_OFFSET_Y = 50f

        // Test-only override for injecting test levels
        var testLevels: List<String>? = null
    }

    private lateinit var gameController: GameController
    private val uiState = mutableStateOf(GameUiState(Position(0, 0), 1))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameController = GameController(this, testLevels)
        uiState.value = GameUiState(gameController.playerPosition, gameController.currentLevel)
        enableEdgeToEdge()
        setContent {
            EinkArcadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        gameController = gameController,
                        uiState = uiState
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> gameController.handleDirectionInput(Direction.DOWN)
            KeyEvent.KEYCODE_DPAD_UP -> gameController.handleDirectionInput(Direction.UP)
            KeyEvent.KEYCODE_DPAD_LEFT -> gameController.handleDirectionInput(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> gameController.handleDirectionInput(Direction.RIGHT)
            KeyEvent.KEYCODE_BUTTON_X -> gameController.restart()
            102 -> gameController.previousLevel() // L
            103 -> gameController.nextLevel() // R
            KeyEvent.KEYCODE_BUTTON_B -> gameController.undo()
            else -> {
                Log.d("GameInput", "KeyDown: $keyCode")
            }
        }
        uiState.value = GameUiState(gameController.playerPosition, gameController.currentLevel)
        return true
    }
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
    uiState: State<GameUiState>
) {
    val playerPosition = uiState.value.playerPosition
    val levelNumber = uiState.value.levelNumber

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = "Level $levelNumber",
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
    }

    val isGameWon by remember(playerPosition) {
        derivedStateOf { gameController.isGameWon }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        for (row in 0 until gameController.gridHeight) {
            for (col in 0 until gameController.gridWidth) {
                val x = MainActivity.GRID_OFFSET_X + col * MainActivity.CELL_SIZE
                val y = MainActivity.GRID_OFFSET_Y + row * MainActivity.CELL_SIZE
                val tile = gameController.getTileAt(Position(row, col))
                when (tile) {
                    Tile.WALL -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(
                                MainActivity.CELL_SIZE,
                                MainActivity.CELL_SIZE
                            )
                        )
                    }

                    Tile.BOX -> {
                        val padding = MainActivity.CELL_SIZE * 0.2f
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.DarkGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x + padding, y + padding),
                            size = androidx.compose.ui.geometry.Size(
                                MainActivity.CELL_SIZE - 2 * padding,
                                MainActivity.CELL_SIZE - 2 * padding
                            )
                        )
                    }

                    Tile.BOX_ON_TARGET -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.LightGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(
                                MainActivity.CELL_SIZE,
                                MainActivity.CELL_SIZE
                            )
                        )
                        val padding = MainActivity.CELL_SIZE * 0.2f
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.DarkGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x + padding, y + padding),
                            size = androidx.compose.ui.geometry.Size(
                                MainActivity.CELL_SIZE - 2 * padding,
                                MainActivity.CELL_SIZE - 2 * padding
                            )
                        )
                    }

                    Tile.TARGET -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.LightGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(
                                MainActivity.CELL_SIZE,
                                MainActivity.CELL_SIZE
                            )
                        )
                    }

                    else -> {}
                }
                if (row == playerPosition.row && col == playerPosition.col) {
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.Gray,
                        radius = MainActivity.CELL_SIZE * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(
                            x + MainActivity.CELL_SIZE / 2,
                            y + MainActivity.CELL_SIZE / 2
                        )
                    )
                }
            }
        }
    }

    if (isGameWon) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You win!",
                color = androidx.compose.ui.graphics.Color.Black,
                fontSize = 32.sp
            )
        }
    }
}


class GameEngine(level: Level) {
    private var gameState = GameState.fromLevel(level)
    var playerPosition = level.playerStart
        private set
    private var lastSavedState: Pair<GameState, Position>? = null

    val isGameWon: Boolean
        get() = gameState.grid.flatten().none { it == Tile.TARGET }

    fun move(direction: Direction) {
        if (isGameWon) return

        val newPosition = playerPosition.move(direction)
        val targetTile = gameState.tileAt(newPosition) ?: return
        if (targetTile == Tile.WALL) return

        if (targetTile == Tile.BOX || targetTile == Tile.BOX_ON_TARGET) {
            val boxNewPosition = newPosition.move(direction)
            if (
                gameState.isInBounds(boxNewPosition) &&
                (gameState.tileAt(boxNewPosition) == Tile.EMPTY || gameState.tileAt(boxNewPosition) == Tile.TARGET)
            ) {
                lastSavedState = Pair(
                    gameState.copy(grid = gameState.grid.map { it.toMutableList() }),
                    playerPosition
                )
                // Move box
                val leavingTile = targetTile
                val destinationTile = gameState.tileAt(boxNewPosition)

                gameState.grid[newPosition.row][newPosition.col] =
                    if (leavingTile == Tile.BOX_ON_TARGET) Tile.TARGET else Tile.EMPTY

                gameState.grid[boxNewPosition.row][boxNewPosition.col] =
                    if (destinationTile == Tile.TARGET) Tile.BOX_ON_TARGET else Tile.BOX
            } else {
                return
            }
        }

        playerPosition = newPosition
    }

    fun getTileAt(position: Position): Tile? {
        return gameState.tileAt(position)
    }

    fun undo() {
        val savedState = lastSavedState ?: return
        gameState = GameState(savedState.first.grid.map { it.toMutableList() })
        playerPosition = savedState.second
        lastSavedState = null
    }
}