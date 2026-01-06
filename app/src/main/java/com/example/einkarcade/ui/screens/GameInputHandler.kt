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
        val selectedBox = selection.getSelectedBox()

        fun attemptBoxMove(selectedBox: Position) {
            gameController.moveBoxTo(selectedBox, tappedPosition)
        }

        val tile = gameController.tiles[tappedPosition.row][tappedPosition.col]

        if (tile == Tile.WALL) {
            if (selectedBox != null) {
                selection.setSelectedBox(null)
                attemptBoxMove(selectedBox)
            }
            return
        }

        if (gameController.boxPositions.contains(tappedPosition)) {
            if (selectedBox == tappedPosition) {
                selection.setSelectedBox(null)
            } else {
                selection.setSelectedBox(tappedPosition)
            }
        } else if (selectedBox != null) {
            selection.setSelectedBox(null)
            attemptBoxMove(selectedBox)
        } else {
            gameController.movePlayerTo(tappedPosition)
        }
    }
}
