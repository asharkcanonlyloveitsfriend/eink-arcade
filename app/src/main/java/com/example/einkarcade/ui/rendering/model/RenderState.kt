package com.example.einkarcade.ui.rendering.model

import android.graphics.Rect
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.LevelTransition

internal data class RenderState(
    var tiles: List<List<Tile>> = emptyList(),
    var boxPositions: Set<Position> = emptySet(),
    var playerPosition: Position? = null,
    var displayedPlayerPosition: Position? = null,
    var pendingPlayerPosition: Position? = null,
    var selectedBox: Position? = null,
    var isFacingLeft: Boolean = false,
    var pendingFacingLeft: Boolean? = null,
    var isInitialized: Boolean = false
)

internal data class AnimationState(
    var boxPath: List<Position> = emptyList(),
    var boxPathActive: Boolean = false,
    var boxPathShrink: Float = 0f,
    var boxPathStartMs: Long = 0L,
    var boxPathDirtyRect: Rect? = null,
    var boxPathNeedsFinalClear: Boolean = false,
    var boxPathSuppressLine: Boolean = false,
    var playerSilhouettePosition: Position? = null,
    var playerSilhouetteStartMs: Long = 0L,
    var playerFlashPosition: Position? = null,
    var playerFlashStartMs: Long = 0L,
    var boxFlashPosition: Position? = null,
    var boxFlashStartMs: Long = 0L,
    var blinkStartMs: Long = 0L,
    var blinkEndMs: Long = 0L,
    var lastBlinkActive: Boolean = false,
    var vanishPosition: Position? = null,
    var vanishStartMs: Long = 0L,
    var vanishStep: Int? = null,
    var vanishLastPosition: Position? = null,
    var vanishNeedsFinalClear: Boolean = false
)

internal data class TransitionState(
    var transition: LevelTransition? = null
)
