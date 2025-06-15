package com.example.einkarcade

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.einkarcade.ui.theme.EinkArcadeTheme

enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class Tile { EMPTY, PLAYER }

class MainActivity : ComponentActivity() {

    companion object {
        internal const val CELL_SIZE = 100f
        internal const val GAME_LEFT = 50f
        internal const val GAME_TOP = 0f
        internal const val GRID_WIDTH = 6
        internal const val GRID_HEIGHT = 6
        internal const val GAME_WIDTH = CELL_SIZE * GRID_WIDTH
        internal const val GAME_HEIGHT = CELL_SIZE * GRID_HEIGHT
        internal val LEVEL_GRID = mutableStateOf(
            Array(GRID_HEIGHT) { row ->
                Array(GRID_WIDTH) { col ->
                    if (row == 0 && col == 0) Tile.PLAYER else Tile.EMPTY
                }
            }
        )
    }


    private var playerRow = 0
    private var playerCol = 0

    private fun move(direction: Direction) {
        Log.d("GameInput", "Move: $direction")

        val (newRow, newCol) = when (direction) {
            Direction.UP -> playerRow - 1 to playerCol
            Direction.DOWN -> playerRow + 1 to playerCol
            Direction.LEFT -> playerRow to playerCol - 1
            Direction.RIGHT -> playerRow to playerCol + 1
        }

        // Check bounds
        if (newRow !in 0 until GRID_HEIGHT || newCol !in 0 until GRID_WIDTH) return

        val oldGrid = LEVEL_GRID.value
        if (oldGrid[newRow][newCol] != Tile.EMPTY) return

        val newGrid = oldGrid.mapIndexed { row, rowData ->
            rowData.mapIndexed { col, tile ->
                when {
                    row == playerRow && col == playerCol -> Tile.EMPTY
                    row == newRow && col == newCol -> Tile.PLAYER
                    else -> tile
                }
            }.toTypedArray()
        }.toTypedArray()

        playerRow = newRow
        playerCol = newCol
        LEVEL_GRID.value = newGrid
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
                Log.d("GameInput", "KeyDown: $keyCode")
            }
        }
        return true
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        drawRect(
            color = androidx.compose.ui.graphics.Color.LightGray,
            topLeft = androidx.compose.ui.geometry.Offset(MainActivity.GAME_LEFT, MainActivity.GAME_TOP),
            size = androidx.compose.ui.geometry.Size(MainActivity.GAME_WIDTH, MainActivity.GAME_HEIGHT),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )
        for (row in 0 until MainActivity.GRID_HEIGHT) {
            for (col in 0 until MainActivity.GRID_WIDTH) {
                val x = MainActivity.GAME_LEFT + col * MainActivity.CELL_SIZE
                val y = MainActivity.GAME_TOP + row * MainActivity.CELL_SIZE
                drawRect(
                    color = androidx.compose.ui.graphics.Color.DarkGray,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
                if (MainActivity.LEVEL_GRID.value[row][col] == Tile.PLAYER) {
                    drawRect(
                        color = androidx.compose.ui.graphics.Color.Gray,
                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(MainActivity.CELL_SIZE, MainActivity.CELL_SIZE)
                    )
                }
            }
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