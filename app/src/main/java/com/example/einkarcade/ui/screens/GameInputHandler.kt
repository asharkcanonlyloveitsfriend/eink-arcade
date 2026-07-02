package com.example.einkarcade.ui.screens

import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position

internal object GameInputHandler {
    fun handleTap(
        tappedPosition: Position,
        gameController: GameController,
        selectedBox: Position?,
    ): Position? {
        if (gameController.tileMap.isVoid(tappedPosition)) {
            if (selectedBox != null) {
                gameController.moveBoxTo(selectedBox, tappedPosition)
            }
            return null
        }
        if (gameController.boxPositions.contains(tappedPosition)) {
            return if (selectedBox == tappedPosition) {
                null
            } else {
                tappedPosition
            }
        }
        if (selectedBox != null) {
            gameController.moveBoxTo(selectedBox, tappedPosition)
        } else {
            gameController.movePlayerTo(tappedPosition)
        }
        return null
    }
}
