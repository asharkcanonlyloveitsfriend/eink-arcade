package com.example.einkarcade

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.einkarcade.sokoban.Direction
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.storage.ensureJsonFromAssetsIfMissing
import com.example.einkarcade.ui.screens.GameScreen
import com.example.einkarcade.ui.theme.EinkArcadeTheme

class MainActivity : ComponentActivity() {

    companion object {
        internal const val CELL_SIZE = 100f
        internal const val GRID_OFFSET_X = 50f
        internal const val GRID_OFFSET_Y = 50f

        // Optional factory for injecting a custom GameController (tests)
        var gameControllerFactory: ((Context) -> GameController)? = null
    }


    private lateinit var gameController: GameController
    private val selectedBoxPosition = mutableStateOf<Position?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On startup: use Downloads copy if present; otherwise seed from assets.
        ensureJsonFromAssetsIfMissing(this)

        gameController = (gameControllerFactory?.invoke(this)) ?: GameController(this, null)
        enableEdgeToEdge()
        setContent {
            EinkArcadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        gameController = gameController,
                        selectedBoxPosition = selectedBoxPosition
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> gameController.step(Direction.DOWN)
            KeyEvent.KEYCODE_DPAD_UP -> gameController.step(Direction.UP)
            KeyEvent.KEYCODE_DPAD_LEFT -> gameController.step(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> gameController.step(Direction.RIGHT)
            KeyEvent.KEYCODE_BUTTON_X -> gameController.restart()
            KeyEvent.KEYCODE_BUTTON_L1 -> gameController.previousLevel()
            KeyEvent.KEYCODE_BUTTON_R1 -> gameController.nextLevel()
            KeyEvent.KEYCODE_BUTTON_B -> gameController.undo()
            else -> {
                Log.d("GameInput", "KeyDown: $keyCode")
            }
        }
        return true
    }
}
