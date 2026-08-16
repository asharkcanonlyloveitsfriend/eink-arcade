package com.example.einkarcade.ui.screens

import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position

internal class StationaryTapTracker(
    private val timeoutMillis: Long,
) {
    private var pendingTap: TimedTap? = null

    fun consumeMatchingTap(
        tappedPosition: Position,
        eventTimeMillis: Long,
    ): Boolean {
        val previousTap = pendingTap
        pendingTap = null

        return previousTap != null &&
            previousTap.position == tappedPosition &&
            eventTimeMillis - previousTap.eventTimeMillis in 0..timeoutMillis
    }

    fun recordTap(
        tappedPosition: Position,
        eventTimeMillis: Long,
        causedEntityMove: Boolean,
    ) {
        pendingTap = if (causedEntityMove) null else TimedTap(tappedPosition, eventTimeMillis)
    }

    fun clear() {
        pendingTap = null
    }

    private data class TimedTap(
        val position: Position,
        val eventTimeMillis: Long,
    )
}

internal object GameInputHandler {
    fun handleTap(
        tappedPosition: Position,
        gameController: GameController,
        selectedBox: Position?,
    ): Position? {
        if (gameController.tileMap.isVoid(tappedPosition)) {
            if (selectedBox != null) {
                gameController.moveBox(selectedBox, tappedPosition)
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
            gameController.moveBox(selectedBox, tappedPosition)
        } else {
            gameController.movePlayerTo(tappedPosition)
        }
        return null
    }
}
