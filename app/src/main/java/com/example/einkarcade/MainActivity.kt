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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.einkarcade.ui.theme.EinkArcadeTheme

enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class Tile { EMPTY, WALL, BOX, TARGET, BOX_ON_TARGET }

data class Position(val row: Int, val col: Int)
data class GameState(
    val grid: List<MutableList<Tile>>
) {
    fun isInBounds(position: Position): Boolean {
        return position.row in grid.indices && position.col in grid[0].indices
    }
    fun tileAt(position: Position): Tile? {
        return if (isInBounds(position)) grid[position.row][position.col] else null
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        internal const val CELL_SIZE = 100f
        internal const val GAME_LEFT = 50f
        internal const val GAME_TOP = 0f
        internal const val GRID_WIDTH = 6
        internal const val GRID_HEIGHT = 6
        internal const val GAME_WIDTH = CELL_SIZE * GRID_WIDTH
        internal const val GAME_HEIGHT = CELL_SIZE * GRID_HEIGHT
        internal val PLAYER_POSITION = mutableStateOf(Position(0, 0))
        internal val GAME_STATE = GameState(
            grid = List(GRID_HEIGHT) { row ->
                MutableList(GRID_WIDTH) { col ->
                    when {
                        row == 2 && col == 2 -> Tile.WALL
                        row == 3 && col == 3 -> Tile.BOX
                        row == 4 && col == 4 -> Tile.TARGET
                        else -> Tile.EMPTY
                    }
                }
            }
        )
        internal val GAME_WON = mutableStateOf(false)
    }

    private fun randomBoxPosition(): Position {
        val possiblePositions = listOf(
            Position(1, 1), Position(1, 2), Position(1, 3),
            Position(2, 1), Position(2, 3), Position(3, 1),
            Position(3, 2)
        )
        return possiblePositions.random()
    }

    private fun move(direction: Direction) {
        if (GAME_WON.value) return
        Log.d("GameInput", "Move: $direction")

        val (playerRow, playerCol) = PLAYER_POSITION.value

        val newPosition = when (direction) {
            Direction.UP -> Position(playerRow - 1, playerCol)
            Direction.DOWN -> Position(playerRow + 1, playerCol)
            Direction.LEFT -> Position(playerRow, playerCol - 1)
            Direction.RIGHT -> Position(playerRow, playerCol + 1)
        }

        val targetTile = GAME_STATE.tileAt(newPosition) ?: return
        if (targetTile == Tile.WALL) return

        if (targetTile == Tile.BOX) {
            val boxNewPosition = when (direction) {
                Direction.UP -> Position(newPosition.row - 1, newPosition.col)
                Direction.DOWN -> Position(newPosition.row + 1, newPosition.col)
                Direction.LEFT -> Position(newPosition.row, newPosition.col - 1)
                Direction.RIGHT -> Position(newPosition.row, newPosition.col + 1)
            }
            if (
                GAME_STATE.isInBounds(boxNewPosition) &&
                GAME_STATE.tileAt(boxNewPosition) == Tile.EMPTY
            ) {
                GAME_STATE.grid[newPosition.row][newPosition.col] = Tile.EMPTY
                GAME_STATE.grid[boxNewPosition.row][boxNewPosition.col] = Tile.BOX
            } else if (
                GAME_STATE.isInBounds(boxNewPosition) &&
                GAME_STATE.tileAt(boxNewPosition) == Tile.TARGET
            ) {
                GAME_STATE.grid[newPosition.row][newPosition.col] = Tile.EMPTY
                GAME_STATE.grid[boxNewPosition.row][boxNewPosition.col] = Tile.BOX_ON_TARGET
            } else {
                return
            }
        }

        PLAYER_POSITION.value = newPosition

        val allCovered = GAME_STATE.grid.flatten().none { it == Tile.TARGET }
        if (allCovered) {
            GAME_WON.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EinkArcadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> move(Direction.DOWN)
            KeyEvent.KEYCODE_DPAD_UP -> move(Direction.UP)
            KeyEvent.KEYCODE_DPAD_LEFT -> move(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> move(Direction.RIGHT)
            else -> {
                if (GAME_WON.value) {
                    PLAYER_POSITION.value = Position(0, 0)
                    GAME_WON.value = false
                    // Reset the box position
                    val newBoxPos = randomBoxPosition()
                    for (row in 0 until GRID_HEIGHT) {
                        for (col in 0 until GRID_WIDTH) {
                            if (GAME_STATE.grid[row][col] == Tile.BOX_ON_TARGET) {
                                GAME_STATE.grid[row][col] = Tile.TARGET
                            }
                        }
                    }
                    GAME_STATE.grid[newBoxPos.row][newBoxPos.col] = Tile.BOX
                } else {
                    Log.d("GameInput", "KeyDown: $keyCode")
                }
            }
        }
        return true
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            color = androidx.compose.ui.graphics.Color.DarkGray,
            topLeft = androidx.compose.ui.geometry.Offset(MainActivity.GAME_LEFT, MainActivity.GAME_TOP),
            size = androidx.compose.ui.geometry.Size(MainActivity.GAME_WIDTH, MainActivity.GAME_HEIGHT),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        val player = MainActivity.PLAYER_POSITION.value
        for (row in 0 until MainActivity.GRID_HEIGHT) {
            for (col in 0 until MainActivity.GRID_WIDTH) {
                val x = MainActivity.GAME_LEFT + col * MainActivity.CELL_SIZE
                val y = MainActivity.GAME_TOP + row * MainActivity.CELL_SIZE
                drawRect(
                    color = androidx.compose.ui.graphics.Color.LightGray,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
                val tile = MainActivity.GAME_STATE.grid[row][col]
                if (tile == Tile.WALL) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                    )
                } else if (tile == Tile.BOX) {
                    val padding = MainActivity.CELL_SIZE * 0.2f
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.DarkGray,
                        topLeft = androidx.compose.ui.geometry.Offset(x + padding, y + padding),
                        size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE - 2 * padding, MainActivity.CELL_SIZE - 2 * padding)
                    )
                } else if (tile == Tile.BOX_ON_TARGET) {
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
                } else if (tile == Tile.TARGET) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.LightGray,
                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                    )
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
    if (MainActivity.GAME_WON.value) {
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
        GameScreen()
    }
}