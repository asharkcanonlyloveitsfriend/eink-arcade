package com.example.einkarcade.ui.screens

import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile

internal class GameUiState(
    var selectedBox: Position? = null,
    var isFacingLeft: Boolean = false,
    var lastBackTapTimeMs: Long? = null,
    val doubleTapWindowMs: Long = 350L,
    var blinkPulse: Int = 0
)

internal class GameAnimState(
    val boxPathAnimation: BoxPathAnimationState,
    val vanishAnimation: VanishAnimationState
)

internal object GameInputHandler {
    fun handleBackKeyUp(
        nowMs: Long,
        gameController: GameController,
        ui: GameUiState,
        resetSelection: () -> Unit
    ) {
        val lastTap = ui.lastBackTapTimeMs
        if (lastTap != null && nowMs - lastTap <= ui.doubleTapWindowMs) {
            ui.lastBackTapTimeMs = null
            resetSelection()
            gameController.restart()
        } else {
            ui.lastBackTapTimeMs = nowMs
            ui.isFacingLeft = false
            gameController.undo()
        }
    }

    fun handleTap(
        tappedPosition: Position,
        gameController: GameController,
        ui: GameUiState,
        anim: GameAnimState
    ) {
        fun attemptBoxMove(selectedBox: Position) {
            val boxPath = gameController.moveBoxTo(selectedBox, tappedPosition)
            if (boxPath == null) {
                ui.blinkPulse += 1
                return
            }
            val previous = boxPath[boxPath.size - 2]
            val current = boxPath.last()
            val pushLeft = previous.row == current.row && current.col < previous.col
            if (!pushLeft) {
                ui.isFacingLeft = false
            }
            val lastPosition = boxPath.last()
            if (gameController.tiles[lastPosition.row][lastPosition.col] == Tile.WALL) {
                anim.vanishAnimation.start(lastPosition)
                ui.blinkPulse += 1
            }
            anim.boxPathAnimation.start(boxPath, gameController.playerPosition) {
                ui.isFacingLeft = pushLeft
            }
        }

        val tile = gameController.tiles[tappedPosition.row][tappedPosition.col]
        val selectedBox = ui.selectedBox

        if (tile == Tile.WALL) {
            if (selectedBox != null) {
                ui.selectedBox = null
                attemptBoxMove(selectedBox)
            }
            return
        }

        if (gameController.boxPositions.contains(tappedPosition)) {
            if (selectedBox == tappedPosition) {
                ui.selectedBox = null
            } else {
                ui.selectedBox = tappedPosition
            }
        } else if (selectedBox != null) {
            ui.selectedBox = null
            attemptBoxMove(selectedBox)
        } else {
            gameController.movePlayerTo(tappedPosition)
            ui.isFacingLeft = false
        }
    }
}
