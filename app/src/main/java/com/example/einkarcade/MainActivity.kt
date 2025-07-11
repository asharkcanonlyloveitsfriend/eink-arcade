package com.example.einkarcade

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.focusProperties
import com.example.einkarcade.sokoban.BoxMover
import com.example.einkarcade.sokoban.Direction
import com.example.einkarcade.sokoban.Pathfinder
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.theme.EinkArcadeTheme

data class Level(
    val grid: List<List<Tile>>,
    val playerStart: Position,
    val boxPositions: Set<Position>
) {
    companion object {
        fun fromAscii(ascii: String): Level {
            val lines = ascii.lines().dropLastWhile { it.isBlank() }
            val maxWidth = lines.maxOfOrNull { it.length } ?: 0
            var playerStart: Position? = null
            val boxes = mutableSetOf<Position>()
            val grid = lines.mapIndexed { rowIndex, line ->
                line.padEnd(maxWidth).mapIndexed { colIndex, char ->
                    val position = Position(rowIndex, colIndex)
                    when (char) {
                        '#' -> Tile.WALL
                        '.' -> Tile.TARGET
                        '$' -> {
                            boxes.add(position)
                            Tile.EMPTY
                        }

                        '*' -> {
                            boxes.add(position)
                            Tile.TARGET
                        }

                        '@' -> {
                            playerStart = position
                            Tile.EMPTY
                        }

                        '+' -> {
                            playerStart = position
                            Tile.TARGET
                        }

                        else -> Tile.EMPTY
                    }
                }
            }
            requireNotNull(playerStart) { "Player start '@' not found in level" }
            return Level(grid, playerStart!!, boxes)
        }
    }

    fun isWall(position: Position): Boolean {
        return grid.getOrNull(position.row)?.getOrNull(position.col) == Tile.WALL
    }

    fun isTarget(position: Position): Boolean {
        return grid.getOrNull(position.row)?.getOrNull(position.col) == Tile.TARGET
    }

    fun isPassable(position: Position): Boolean {
        return isInBounds(position) && !isWall(position)
    }

    private fun isInBounds(position: Position): Boolean {
        return position.row in grid.indices && position.col in grid[0].indices
    }
}

data class GameUiState(val playerPosition: Position, val levelNumber: Int)

data class GameState(
    var playerPosition: Position,
    val boxPositions: MutableSet<Position>
) {
    companion object {
        fun fromLevel(level: Level): GameState {
            return GameState(
                playerPosition = level.playerStart,
                boxPositions = level.boxPositions.toMutableSet()
            )
        }
    }

    fun moveBox(from: Position, to: Position) {
        if (!boxPositions.contains(from)) {
            error("No box at position $from")
        }
        boxPositions.remove(from)
        boxPositions.add(to)
    }

    fun movePlayer(to: Position) {
        playerPosition = to
    }

    fun deepCopy(): GameState {
        return GameState(
            playerPosition = playerPosition,
            boxPositions = boxPositions.toMutableSet()
        )
    }
}

class GameController(context: Context, testLevels: List<String>? = null) {
    private val levels: List<String> = testLevels ?: run {
        val levelFiles = context.assets.list("levels")?.toList() ?: emptyList()
        levelFiles.map { filename ->
            context.assets.open("levels/$filename").bufferedReader().use { it.readText() }
        }
    }
    private var currentLevelIndex = 0
    private var level: Level
    private var gameEngine: GameEngine

    init {
        level = Level.fromAscii(levels[currentLevelIndex])
        gameEngine = GameEngine(level)
    }

    val currentLevel: Int
        get() = currentLevelIndex + 1

    val playerPosition: Position
        get() = gameEngine.playerPosition

    val boxPositions: Set<Position>
        get() = gameEngine.boxPositions

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val tiles: List<List<Tile>>
        get() = level.grid

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

    fun moveTo(position: Position) {
        gameEngine.moveTo(position)
    }

    fun moveBoxTo(from: Position, to: Position, playerEnd: Position) {
        gameEngine.moveBoxTo(from, to, playerEnd)
    }

    val walkableGrid: Array<Array<Boolean>>
        get() = gameEngine.walkableGrid
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
    private val selectedBoxPosition = mutableStateOf<Position?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameController = GameController(this, testLevels)
        updateUiState()
        enableEdgeToEdge()
        setContent {
            EinkArcadeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        modifier = Modifier.padding(innerPadding),
                        gameController = gameController,
                        uiState = uiState,
                        selectedBoxPosition = selectedBoxPosition,
                        onStateUpdated = { updateUiState() }
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
            KeyEvent.KEYCODE_BUTTON_L1 -> gameController.previousLevel()
            KeyEvent.KEYCODE_BUTTON_R1 -> gameController.nextLevel()
            KeyEvent.KEYCODE_BUTTON_B -> gameController.undo()
            else -> {
                Log.d("GameInput", "KeyDown: $keyCode")
            }
        }
        updateUiState()
        return true
    }

    private fun updateUiState() {
        uiState.value = GameUiState(
            gameController.playerPosition,
            gameController.currentLevel
        )
    }
}

private fun drawGameObject(
    position: Position,
    draw: (Offset) -> Unit
) {
    draw(position.toOffset())
}

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameController: GameController,
    uiState: State<GameUiState>,
    selectedBoxPosition: MutableState<Position?>,
    onStateUpdated: () -> Unit
) {
    val playerPosition = uiState.value.playerPosition
    val levelNumber = uiState.value.levelNumber

    Box(modifier = modifier.fillMaxSize()) {
        fun handleTap(tappedPosition: Position) {
            val selectedBox = selectedBoxPosition.value

            if (gameController.boxPositions.contains(tappedPosition)) {
                if (selectedBox == tappedPosition) {
                    selectedBoxPosition.value = null
                } else {
                    selectedBoxPosition.value = tappedPosition
                }
            } else if (selectedBox != null) {
                selectedBoxPosition.value = null
                val gridCopy = gameController.walkableGrid.map { it.copyOf() }.toTypedArray()
                gridCopy[selectedBox.row][selectedBox.col] = true
                val boxMover = BoxMover(gridCopy)
                val finalPlayerPosition = boxMover.canMoveBox(
                    selectedBox,
                    tappedPosition,
                    gameController.playerPosition
                )
                if (finalPlayerPosition != null) {
                    gameController.moveBoxTo(selectedBox, tappedPosition, finalPlayerPosition)
                    onStateUpdated()
                }
            } else {
                gameController.moveTo(tappedPosition)
                onStateUpdated()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Level $levelNumber",
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val col = ((offset.x - MainActivity.GRID_OFFSET_X) / MainActivity.CELL_SIZE).toInt()
                            val row = ((offset.y - MainActivity.GRID_OFFSET_Y) / MainActivity.CELL_SIZE).toInt()
                            if (!gameController.isGameWon &&
                                row in gameController.tiles.indices &&
                                col in gameController.tiles[0].indices
                            ) {
                                handleTap(Position(row, col))
                            }
                        }
                    }
            ) {
                for ((rowIndex, row) in gameController.tiles.withIndex()) {
                    for ((colIndex, tile) in row.withIndex()) {
                        when (tile) {
                            Tile.WALL -> {
                                drawGameObject(Position(rowIndex, colIndex)) { offset ->
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = offset,
                                        size = Size(
                                            MainActivity.CELL_SIZE,
                                            MainActivity.CELL_SIZE
                                        )
                                    )
                                }
                            }
                            Tile.TARGET -> {
                                drawGameObject(Position(rowIndex, colIndex)) { offset ->
                                    drawRect(
                                        color = Color.LightGray,
                                        topLeft = offset,
                                        size = Size(
                                            MainActivity.CELL_SIZE,
                                            MainActivity.CELL_SIZE
                                        )
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }

                for (position in gameController.boxPositions) {
                    val padding = MainActivity.CELL_SIZE * 0.2f
                    val color = if (position == selectedBoxPosition.value) Color.Black else Color.Gray
                    drawGameObject(position) { offset ->
                        drawRect(
                            color = color,
                            topLeft = Offset(offset.x + padding, offset.y + padding),
                            size = Size(
                                MainActivity.CELL_SIZE - 2 * padding,
                                MainActivity.CELL_SIZE - 2 * padding
                            )
                        )
                    }
                }

                drawGameObject(playerPosition) { offset ->
                    drawCircle(
                        color = Color.Gray,
                        radius = MainActivity.CELL_SIZE * 0.4f,
                        center = Offset(
                            offset.x + MainActivity.CELL_SIZE / 2,
                            offset.y + MainActivity.CELL_SIZE / 2
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { gameController.restart(); onStateUpdated() },
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Text("Restart")
                }
                Button(
                    onClick = { gameController.undo(); onStateUpdated() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Text("Undo")
                }
                Button(
                    onClick = { gameController.previousLevel(); onStateUpdated() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = { gameController.nextLevel(); onStateUpdated() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Text("Next")
                }
            }
        }

        if (gameController.isGameWon) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.White)
                        .border(width = 2.dp, color = Color.Black)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "You win!",
                        color = Color.Black,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}


class GameEngine(private val level: Level) {
    private var gameState = GameState.fromLevel(level)
    private var lastSavedState: GameState? = null

    val playerPosition: Position
        get() = gameState.playerPosition

    val boxPositions: Set<Position>
        get() = gameState.boxPositions

    val isGameWon: Boolean
        get() = gameState.boxPositions.all { level.isTarget(it) }

    fun move(direction: Direction) {
        if (isGameWon) return

        val targetPosition = playerPosition.move(direction)
        if (level.isWall(targetPosition)) return

        if (hasBoxAt(targetPosition)) {
            if (canPushBox(targetPosition, direction)) {
                pushBox(targetPosition, direction)
            }
        } else {
            gameState.movePlayer(targetPosition)
        }
    }

    private fun canPushBox(boxPosition: Position, direction: Direction): Boolean {
        val newBoxPosition = boxPosition.move(direction)
        return level.isPassable(newBoxPosition) && !hasBoxAt(newBoxPosition)
    }

    private fun pushBox(boxPosition: Position, direction: Direction) {
        val newBoxPosition = boxPosition.move(direction)
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(boxPosition, newBoxPosition)
        gameState.movePlayer(boxPosition)
    }

    private fun hasBoxAt(position: Position): Boolean {
        return gameState.boxPositions.contains(position)
    }

    fun undo() {
        val savedState = lastSavedState ?: return
        gameState = savedState.deepCopy()
        lastSavedState = null
    }

    fun moveBoxTo(from: Position, to: Position, playerEnd: Position) {
        lastSavedState = gameState.deepCopy()
        gameState.moveBox(from, to)
        gameState.movePlayer(playerEnd)
    }

    fun moveTo(position: Position) {
        if (hasBoxAt(position) && isAdjacent(playerPosition, position)) {
            val direction = directionTo(playerPosition, position)
            if (direction != null) {
                move(direction)
            }
        } else {
            val pathfinder = Pathfinder(walkableGrid)
            if (pathfinder.canFindPath(playerPosition, position)) {
                gameState.movePlayer(position)
            }
        }
    }

    private fun isAdjacent(a: Position, b: Position): Boolean {
        val rowDiff = kotlin.math.abs(a.row - b.row)
        val colDiff = kotlin.math.abs(a.col - b.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    private fun directionTo(from: Position, to: Position): Direction? {
        return when {
            from.row == to.row && from.col + 1 == to.col -> Direction.RIGHT
            from.row == to.row && from.col - 1 == to.col -> Direction.LEFT
            from.row + 1 == to.row && from.col == to.col -> Direction.DOWN
            from.row - 1 == to.row && from.col == to.col -> Direction.UP
            else -> null
        }
    }

    val walkableGrid: Array<Array<Boolean>>
        get() = Array(level.grid.size) { row ->
            Array(level.grid[0].size) { col ->
                val pos = Position(row, col)
                level.grid[row][col] != Tile.WALL && !gameState.boxPositions.contains(pos)
            }
        }
}
// Extension function to convert Position to Offset based on MainActivity constants
private fun Position.toOffset(): Offset {
    return Offset(
        MainActivity.GRID_OFFSET_X + this.col * MainActivity.CELL_SIZE,
        MainActivity.GRID_OFFSET_Y + this.row * MainActivity.CELL_SIZE
    )
}