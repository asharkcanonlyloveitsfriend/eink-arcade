package com.example.einkarcade


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.einkarcade.sokoban.BoxMover
import com.example.einkarcade.sokoban.Direction
import com.example.einkarcade.sokoban.GameEngine
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.theme.EinkArcadeTheme
import org.json.JSONArray
import org.json.JSONObject



data class GameUiState(val playerPosition: Position, val levelName: String)

data class LevelSet(
    val id: String,
    val name: String,
    val levels: List<Level>
)


// JSON store for Downloads/EinkArcade/levels.txt.
private class JsonStore(private val context: Context) {
    private val cr get() = context.contentResolver
    private val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.RELATIVE_PATH
    )
    private fun findUri(): Uri? {
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val args = arrayOf(MainActivity.LEVELS_JSON_NAME, MainActivity.LEVELS_DIR_RELATIVE_PATH)
        return cr.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            } else null
        }
    }
    fun readText(): String? {
        val uri = findUri() ?: return null
        return cr.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }
    fun writeText(text: String): Boolean {
        val uri = findUri() ?: return false
        return try {
            cr.openOutputStream(uri, "w")?.use { os ->
                os.write(text.toByteArray())
                os.flush()
            }
            true
        } catch (t: Throwable) {
            Log.e("JsonStore", "write failed", t)
            false
        }
    }
}

// Central repository for loading and saving level sets as a whole.
private class LevelsRepository(context: Context) {
    private val jsonStore = JsonStore(context)

    fun loadSets(): List<LevelSet>? {
        val jsonText = jsonStore.readText() ?: return null
        val root = JSONObject(jsonText)
        val setsArr = root.getJSONArray("sets")
        val out = mutableListOf<LevelSet>()
        for (i in 0 until setsArr.length()) {
            val setObj = setsArr.getJSONObject(i)
            val setId = setObj.getString("id")
            val setName = setObj.getString("name")

            val levelsArr = setObj.getJSONArray("levels")
            val levels = mutableListOf<Level>()
            for (j in 0 until levelsArr.length()) {
                val lvl = levelsArr.getJSONObject(j)
                val name = lvl.getString("name")
                val ascii = lvl.getString("ascii")
                val level = Level.fromAscii(name, ascii)
                level.setRating(lvl.optInt("rating", 0))
                level.setCompletedAt(lvl.optLong("completedAt", 0L))
                levels.add(level)
            }
            out.add(LevelSet(id = setId, name = setName, levels = levels))
        }
        return out
    }


    // Build full JSON from the in-memory model using Level.ascii.
    fun saveAllFromSets(sets: List<LevelSet>): Boolean {
        return try {
            val outRoot = JSONObject()
            val outSets = JSONArray()
            for (set in sets) {
                if (set.levels.isEmpty()) continue
                val outSet = JSONObject()
                outSet.put("id", set.id)
                outSet.put("name", set.name)

                val outLevels = JSONArray()
                set.levels.forEach { lvl ->
                    val obj = JSONObject()
                    obj.put("name", lvl.name)
                    obj.put("ascii", lvl.ascii)
                    obj.put("rating", lvl.rating)
                    obj.put("completedAt", lvl.completedAt)
                    outLevels.put(obj)
                }
                outSet.put("levels", outLevels)
                outSets.put(outSet)
            }
            outRoot.put("sets", outSets)
            jsonStore.writeText(outRoot.toString())
        } catch (t: Throwable) {
            Log.e("LevelsRepository", "saveAllFromSets failed", t)
            false
        }
    }
}

class GameController(context: Context, testLevels: List<String>? = null) {

    private val repository = LevelsRepository(context)

    private fun persistLevelChanges() {
        Thread { repository.saveAllFromSets(levelSets) }.start()
    }

    private fun recordCompletionIfWon() {
        if (gameEngine.isGameWon) {
            level.markCompleted()
            persistLevelChanges()
        }
    }

    private fun loadLevelSets(context: Context): List<LevelSet>? = repository.loadSets()

    private val levelSets: List<LevelSet> = testLevels?.let {
        listOf(
            LevelSet(
                id = "test",
                name = "test",
                levels = it.mapIndexed { index, content ->
                    Level.fromAscii("TestLevel$index", content)
                }
            )
        )
    } ?: (loadLevelSets(context) ?: emptyList())


    val availableSets: List<String>
        get() = levelSets.map { it.name }

    // Levels for current set.
    fun levels(): List<Level> = levelsInCurrentSet

    fun getCurrentRating(): Int = level.rating
    fun toggleThumbUp() {
        level.toggleThumbUp()
        persistLevelChanges()
    }
    fun toggleThumbDown() {
        level.toggleThumbDown()
        persistLevelChanges()
    }

    private var currentSetIndex: Int = 0
    private var currentLevelIndex = 0
    private var level: Level
    private var gameEngine: GameEngine
    val currentSetName: String
        get() = levelSets[currentSetIndex].name

    private val levelsInCurrentSet: List<Level>
        get() = levelSets[currentSetIndex].levels

    val availableLevels: List<String>
        get() = levelsInCurrentSet.map { it.name }
    fun selectSet(setName: String) {
        if (!levelSets.any { it.name == setName }) return
        val idx = levelSets.indexOfFirst { it.name == setName }
        if (idx == -1) return
        currentSetIndex = idx
        currentLevelIndex = 0
        level = levelsInCurrentSet[currentLevelIndex]
        gameEngine = GameEngine(level)
    }
    fun selectLevel(name: String) {
        val index = levelsInCurrentSet.indexOfFirst { it.name == name }
        if (index != -1) {
            currentLevelIndex = index
            level = levelsInCurrentSet[currentLevelIndex]
            gameEngine = GameEngine(level)
        }
    }

    init {
        level = levelsInCurrentSet[currentLevelIndex]
        gameEngine = GameEngine(level)
    }


    val playerPosition: Position
        get() = gameEngine.playerPosition

    val boxPositions: Set<Position>
        get() = gameEngine.boxPositions

    val isGameWon: Boolean
        get() = gameEngine.isGameWon

    val tiles: List<List<Tile>>
        get() = level.grid

    val levelName: String
        get() = level.name

    fun restart() {
        if (isGameWon) {
            nextLevel()
        } else {
            gameEngine = GameEngine(level)
        }
    }

    fun handleDirectionInput(direction: Direction) {
        gameEngine.move(direction)
        recordCompletionIfWon()
    }

    fun nextLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = (currentLevelIndex + 1) % levels.size
        level = levels[currentLevelIndex]
        gameEngine = GameEngine(level)
    }

    fun previousLevel() {
        val levels = levelsInCurrentSet
        currentLevelIndex = if (currentLevelIndex - 1 < 0) levels.size - 1 else currentLevelIndex - 1
        level = levels[currentLevelIndex]
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
        recordCompletionIfWon()
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

        internal const val LEVELS_DIR_RELATIVE_PATH = "Download/EinkArcade/"
        internal const val LEVELS_JSON_NAME = "levels.txt"
        private const val DEFAULT_LEVELS_ASSET = "default_levels.json"
    }

    // Ensure Downloads/EinkArcade/levels.txt exists; seed from assets if missing.
    private fun ensureJsonFromAssetsIfMissing(context: Context, assetPath: String = DEFAULT_LEVELS_ASSET): Uri? {
        val cr = context.contentResolver

        // Find existing file
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.RELATIVE_PATH
        )
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(LEVELS_JSON_NAME, LEVELS_DIR_RELATIVE_PATH)
        cr.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idCol)
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            }
        }

        // Create and seed from asset
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, LEVELS_JSON_NAME)
            put(MediaStore.Downloads.RELATIVE_PATH, LEVELS_DIR_RELATIVE_PATH)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }
        val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val seed = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        cr.openOutputStream(uri, "w")?.use { os ->
            os.write(seed.toByteArray())
            os.flush()
        }
        return uri
    }


    private lateinit var gameController: GameController
    private val uiState = mutableStateOf(GameUiState(Position(0, 0), ""))
    private val selectedBoxPosition = mutableStateOf<Position?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On startup: use Downloads copy if present; otherwise seed it from assets/default_levels.json
        ensureJsonFromAssetsIfMissing(this)

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
            gameController.levelName
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
    val selectedLevel = remember { mutableStateOf(uiState.value.levelName) }
    selectedLevel.value = uiState.value.levelName
    // Tick to force recomposition on rating change.
    val ratingTick = remember(gameController.currentSetName, selectedLevel.value) { mutableStateOf(0) }

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
            val setExpanded = remember { mutableStateOf(false) }
            val selectedSet = remember { mutableStateOf(gameController.currentSetName) }
            val sets = gameController.availableSets.toList()

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable { setExpanded.value = true }
            ) {
                Text("Set: ${selectedSet.value}", fontSize = 24.sp)
                DropdownMenu(
                    expanded = setExpanded.value,
                    onDismissRequest = { setExpanded.value = false }
                ) {
                    sets.forEach { setName ->
                        DropdownMenuItem(
                            text = { Text(setName) },
                            onClick = {
                                gameController.selectSet(setName)
                                selectedSet.value = setName
                                selectedLevel.value = gameController.availableLevels.firstOrNull() ?: ""
                                setExpanded.value = false
                                onStateUpdated()
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            val levelExpanded = remember { mutableStateOf(false) }
            // Levels for dropdown with ratings
            val levels = gameController.levels()
            val _forceLevels = ratingTick.value

            Box(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp)
                    .clickable { levelExpanded.value = true }
            ) {
                Text("Level: ${selectedLevel.value}", fontSize = 24.sp)
                DropdownMenu(
                    expanded = levelExpanded.value,
                    onDismissRequest = { levelExpanded.value = false }
                ) {
                    levels.forEach { lvl ->
                        val completedMark = if (lvl.isCompleted) " ✓" else ""
                        val ratingBadge = when (lvl.rating) { 1 -> " 👍"; -1 -> " 👎"; else -> "" }
                        DropdownMenuItem(
                            text = { Text(lvl.name + completedMark + ratingBadge) },
                            onClick = {
                                gameController.selectLevel(lvl.name)
                                selectedLevel.value = lvl.name
                                levelExpanded.value = false
                                onStateUpdated()
                            }
                        )
                    }
                }
            }

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
                // Rating buttons with check when selected.
                // Read tick to trigger recomposition
                val _force = ratingTick.value
                val currentRating = gameController.getCurrentRating()

                Button(
                    onClick = { gameController.toggleThumbDown(); ratingTick.value += 1; onStateUpdated() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .focusProperties { canFocus = false }
                ) {
                    val selected = currentRating == -1
                    Text(if (selected) "👎✓" else "👎")
                }
                Button(
                    onClick = { gameController.toggleThumbUp(); ratingTick.value += 1; onStateUpdated() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .focusProperties { canFocus = false }
                ) {
                    val selected = currentRating == 1
                    Text(if (selected) "👍✓" else "👍")
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


private fun Position.toOffset(): Offset {
    return Offset(
        MainActivity.GRID_OFFSET_X + this.col * MainActivity.CELL_SIZE,
        MainActivity.GRID_OFFSET_Y + this.row * MainActivity.CELL_SIZE
    )
}