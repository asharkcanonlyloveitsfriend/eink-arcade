package com.example.einkarcade

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
            var playerStart: Position? = null
            val grid = ascii.trim().lines().mapIndexed { rowIndex, line ->
                line.mapIndexed { colIndex, char ->
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

class GameController {
    private val levels = listOf(
        """
        ######
        #    #
        # @$ #
        #   .#
        ######
        """.trimIndent(),
        """
        ######
        #  @ #
        #    #
        # $$ #
        # .. #
        ######
        """.trimIndent()
    )
    private var currentLevelIndex = 0
    private var level = Level.fromAscii(levels[currentLevelIndex])
    private var gameEngine = GameEngine(level)

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
}

class MainActivity : ComponentActivity() {

    companion object {
        internal const val CELL_SIZE = 100f
        internal const val GAME_LEFT = 50f
        internal const val GAME_TOP = 0f
    }

    private val gameController = GameController()
    private val playerPositionState = mutableStateOf(gameController.playerPosition)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EinkArcadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        gameController = gameController,
                        playerPositionState = playerPositionState
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
            else -> {
                Log.d("GameInput", "KeyDown: $keyCode")
            }
        }
        playerPositionState.value = gameController.playerPosition
        return true
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier, gameController: GameController, playerPositionState: State<Position>) {
    val isGameWon by remember(playerPositionState.value) {
        derivedStateOf { gameController.isGameWon }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val gameWidth = MainActivity.CELL_SIZE * gameController.gridWidth
        val gameHeight = MainActivity.CELL_SIZE * gameController.gridHeight
        drawRect(
            color = androidx.compose.ui.graphics.Color.DarkGray,
            topLeft = androidx.compose.ui.geometry.Offset(MainActivity.GAME_LEFT, MainActivity.GAME_TOP),
            size = androidx.compose.ui.geometry.Size(gameWidth, gameHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        val player = playerPositionState.value
        for (row in 0 until gameController.gridHeight) {
            for (col in 0 until gameController.gridWidth) {
                val x = MainActivity.GAME_LEFT + col * MainActivity.CELL_SIZE
                val y = MainActivity.GAME_TOP + row * MainActivity.CELL_SIZE
                drawRect(
                    color = androidx.compose.ui.graphics.Color.LightGray,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
                val tile = gameController.getTileAt(Position(row, col))
                when (tile) {
                    Tile.WALL -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                        )
                    }
                    Tile.BOX -> {
                        val padding = MainActivity.CELL_SIZE * 0.2f
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.DarkGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x + padding, y + padding),
                            size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE - 2 * padding, MainActivity.CELL_SIZE - 2 * padding)
                        )
                    }
                    Tile.BOX_ON_TARGET -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.LightGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                        )
                        val padding = MainActivity.CELL_SIZE * 0.2f
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.DarkGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x + padding, y + padding),
                            size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE - 2 * padding, MainActivity.CELL_SIZE - 2 * padding)
                        )
                    }
                    Tile.TARGET -> {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color.LightGray,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                        )
                    }
                    else -> {}
                }
                if (row == player.row && col == player.col) {
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

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    EinkArcadeTheme {
        val playerPositionState = remember { mutableStateOf(Position(0, 0)) }
        GameScreen(gameController = GameController(), playerPositionState = playerPositionState)
    }
}

class GameEngine(level: Level) {
    private var gameState = GameState.fromLevel(level)
    var playerPosition = level.playerStart
        private set

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
}