package com.example.einkarcade.ui.screens

import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
internal object GameInputHandler {
    interface BoxSelection {
        fun getSelectedBox(): Position?
        fun setSelectedBox(position: Position?)
    }

    fun handleBackKeyUp(
        gameController: GameController
    ) {
        val undone = gameController.undo()
        if (undone) {
            return
        }
        if (gameController.isAtStart) {
            gameController.previousLevel()
        } else {
            gameController.restart()
        }
    }

    fun handleTap(
        tappedPosition: Position,
        gameController: GameController,
        selection: BoxSelection
    ) {
        val tile = gameController.tiles[tappedPosition.row][tappedPosition.col]
        if (tile == Tile.WALL) return
        if (gameController.boxPositions.contains(tappedPosition)) {
            val selectedBox = selection.getSelectedBox()
            if (selectedBox == tappedPosition) {
                selection.setSelectedBox(null)
            } else {
                selection.setSelectedBox(tappedPosition)
            }
            return
        }
        selection.setSelectedBox(null)
        gameController.movePlayerTo(tappedPosition)
    }
}
