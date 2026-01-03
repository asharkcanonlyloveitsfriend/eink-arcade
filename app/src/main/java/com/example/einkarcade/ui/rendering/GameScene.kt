package com.example.einkarcade.ui.rendering

import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.screens.BoxPathAnimationState
import com.example.einkarcade.ui.screens.GameUiState
import com.example.einkarcade.ui.screens.VanishAnimationState
import com.example.einkarcade.ui.screens.VanishState

internal data class GameScene(
    val tiles: List<List<Tile>>,
    val boxPositions: Set<Position>,
    val playerPosition: Position,
    val selectedBox: Position?,
    val isFacingLeft: Boolean,
    val isBlinking: Boolean,
    val boxPath: List<Position>,
    val boxPathActive: Boolean,
    val boxPathShrink: Float,
    val vanish: VanishState?
)

internal fun buildGameScene(
    gameController: GameController,
    ui: GameUiState,
    displayedPlayerPosition: Position,
    isBlinking: Boolean,
    boxPathAnimation: BoxPathAnimationState,
    vanishAnimation: VanishAnimationState
): GameScene {
    return GameScene(
        tiles = gameController.tiles,
        boxPositions = gameController.boxPositions,
        playerPosition = displayedPlayerPosition,
        selectedBox = ui.selectedBox,
        isFacingLeft = ui.isFacingLeft,
        isBlinking = isBlinking,
        boxPath = boxPathAnimation.path.value,
        boxPathActive = boxPathAnimation.isActive.value,
        boxPathShrink = boxPathAnimation.shrink.value,
        vanish = vanishAnimation.state.value
    )
}
