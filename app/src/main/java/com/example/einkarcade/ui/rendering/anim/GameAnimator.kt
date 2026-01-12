package com.example.einkarcade.ui.rendering.anim

import android.graphics.Rect
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.RenderTimings
import com.example.einkarcade.ui.rendering.VanishSpec
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.computeBoxPathDirtyRect
import com.example.einkarcade.ui.rendering.geom.computeVanishDirtyRect
import com.example.einkarcade.ui.rendering.geom.spriteDrawParams
import com.example.einkarcade.ui.rendering.model.RenderStateStateful

internal data class AnimationState(
    var boxPath: List<Position> = emptyList(),
    var boxPathActive: Boolean = false,
    var boxPathShrink: Float = 0f,
    var boxPathStartTick: Long = 0L,
    var boxPathDirtyRect: Rect? = null,
    var boxPathNeedsFinalClear: Boolean = false,
    var playerSilhouettePosition: Position? = null,
    var playerSilhouetteStartTick: Long = 0L,
    var playerFlashPosition: Position? = null,
    var playerFlashStartTick: Long = 0L,
    var boxFlashPosition: Position? = null,
    var boxFlashStartTick: Long = 0L,
    var vanishPosition: Position? = null,
    var vanishStartTick: Long = 0L,
    var vanishStep: Int? = null,
    var vanishLastPosition: Position? = null,
    var vanishNeedsFinalClear: Boolean = false
)

internal class GameAnimator(private val assets: AndroidGameAssets) {
    val state = AnimationState()

    fun reset() {
        state.boxPath = emptyList()
        state.boxPathActive = false
        state.boxPathShrink = 0f
        state.boxPathStartTick = 0L
        state.boxPathDirtyRect = null
        state.boxPathNeedsFinalClear = false
        state.playerSilhouettePosition = null
        state.playerSilhouetteStartTick = 0L
        state.playerFlashPosition = null
        state.playerFlashStartTick = 0L
        state.boxFlashPosition = null
        state.boxFlashStartTick = 0L
        state.vanishPosition = null
        state.vanishStartTick = 0L
        state.vanishStep = null
        state.vanishLastPosition = null
        state.vanishNeedsFinalClear = false
    }

    fun startBoxPath(
        path: List<Position>,
        pendingPlayer: Position,
        displayedPlayer: Position,
        viewport: BoardViewport?,
        nowMs: Long,
        renderStateStateful: RenderStateStateful
    ) {
        require(path.size >= 2) { "Box path requires at least two points." }
        val nowTick = RenderTimings.nowTick(nowMs)
        state.boxPath = path
        renderStateStateful.pendingPlayerPosition = pendingPlayer
        state.boxPathStartTick = nowTick
        state.boxPathShrink = 0f
        state.boxPathActive = true
        state.boxPathNeedsFinalClear = false

        state.boxPathDirtyRect = viewport?.let {
            computeBoxPathDirtyRect(it, path, displayedPlayer, pendingPlayer)
        }
    }

    fun startVanish(at: Position, nowMs: Long) {
        val nowTick = RenderTimings.nowTick(nowMs)
        state.vanishPosition = at
        state.vanishStartTick = nowTick
        state.vanishStep = 0
        state.vanishLastPosition = at
        state.vanishNeedsFinalClear = false
    }

    fun startPlayerFlash(from: Position?, nowMs: Long) {
        val nowTick = RenderTimings.nowTick(nowMs)
        state.playerFlashPosition = from
        state.playerFlashStartTick = nowTick
    }

    fun startBoxFlash(from: Position, nowMs: Long) {
        val nowTick = RenderTimings.nowTick(nowMs)
        state.boxFlashPosition = from
        state.boxFlashStartTick = nowTick
    }

    fun startPlayerSilhouette(position: Position?, nowMs: Long) {
        val nowTick = RenderTimings.nowTick(nowMs)
        state.playerSilhouettePosition = position
        state.playerSilhouetteStartTick = nowTick
    }

    fun tick(nowMs: Long, viewport: BoardViewport?, renderStateStateful: RenderStateStateful): TickResult {
        val nowTick = RenderTimings.nowTick(nowMs)
        val changed = updateBoxPathAnimation(nowTick, renderStateStateful)
        val vanishChanged = updateVanishAnimation(nowTick)
        val playerFlashActive =
            state.playerFlashPosition != null &&
                (nowTick - state.playerFlashStartTick) < RenderTimings.FLASH_DURATION_TICKS
        val boxFlashActive =
            state.boxFlashPosition != null &&
                (nowTick - state.boxFlashStartTick) < RenderTimings.FLASH_DURATION_TICKS
        val boxPathDelayEndTick = state.boxPathStartTick + RenderTimings.BOX_PATH_DELAY_TICKS
        val boxPathInDelay = state.boxPathActive && nowTick < boxPathDelayEndTick

        var dirtyRect: Rect? = null
        var requestedRender = false

        if (changed) {
            requestedRender = true
            dirtyRect = computeBoxPathDirtyUnion(nowTick, viewport)
        }

        if (vanishChanged && !changed) {
            requestedRender = true
            dirtyRect = if (state.boxPathActive || state.boxPathNeedsFinalClear) {
                computeBoxPathDirtyUnion(nowTick, viewport)
            } else {
                computeVanishDirtyRect(viewport)
            }
        }

        if (!changed && !vanishChanged && playerFlashActive) {
            requestedRender = true
            dirtyRect = union(dirtyRect, computePlayerFlashDirtyRect(viewport))
        }

        if (!changed && !vanishChanged && boxFlashActive) {
            requestedRender = true
            val position = state.boxFlashPosition
            if (position != null && viewport != null) {
                dirtyRect = union(dirtyRect, spriteDrawParams(viewport, position, 0.90f).dirtyRect)
            }
        }

        if (!playerFlashActive && state.playerFlashPosition != null) {
            requestedRender = true
            val clearedPos = state.playerFlashPosition
            state.playerFlashPosition = null
            state.playerFlashStartTick = 0L
            if (clearedPos != null && viewport != null) {
                dirtyRect = union(dirtyRect, spriteDrawParams(viewport, clearedPos, 0.80f).dirtyRect)
            }
        }

        if (!boxFlashActive && state.boxFlashPosition != null) {
            requestedRender = true
            val clearedPos = state.boxFlashPosition
            state.boxFlashPosition = null
            state.boxFlashStartTick = 0L
            if (clearedPos != null && viewport != null) {
                dirtyRect = union(dirtyRect, spriteDrawParams(viewport, clearedPos, 0.90f).dirtyRect)
            }
        }

        if (state.vanishNeedsFinalClear && state.vanishPosition == null) {
            requestedRender = true
            dirtyRect = union(dirtyRect, computeVanishDirtyRect(viewport))
            state.vanishNeedsFinalClear = false
            state.vanishLastPosition = null
        }

        if (state.boxPathNeedsFinalClear && !state.boxPathActive) {
            state.boxPathDirtyRect = null
            state.boxPathNeedsFinalClear = false
        }

        val vanishActive = state.vanishPosition != null
        var needsNextFrame =
            state.boxPathActive || vanishActive || playerFlashActive || boxFlashActive
        var nextFrameDelayMs: Long? = null

        nextFrameDelayMs = when {
            boxPathInDelay -> RenderTimings.msUntilTick(boxPathDelayEndTick, nowMs)
            needsNextFrame -> RenderTimings.msUntilNextTick(nowMs)
            else -> null
        }
        if (needsNextFrame && nextFrameDelayMs == 0L) {
            nextFrameDelayMs = RenderTimings.TICK_MS
        }

        val forceFullRender = requestedRender && dirtyRect == null

        return TickResult(
            dirtyRect = dirtyRect,
            needsNextFrame = needsNextFrame,
            forceFullRender = forceFullRender,
            nextFrameDelayMs = nextFrameDelayMs
        )
    }

    fun computeBoxPathDirtyUnion(
        nowTick: Long,
        viewport: BoardViewport?
    ): Rect? {
        return computeBoxPathDirtyUnionInternal(nowTick, viewport)
    }

    private fun updateBoxPathAnimation(nowTick: Long, renderStateStateful: RenderStateStateful): Boolean {
        if (!state.boxPathActive) return false
        val elapsedTicks = nowTick - state.boxPathStartTick
        if (elapsedTicks < RenderTimings.BOX_PATH_DELAY_TICKS) return false
        val suppressLine = state.boxPath.size <= 2
        val progress = if (suppressLine) {
            1f
        } else {
            ((elapsedTicks - RenderTimings.BOX_PATH_DELAY_TICKS).toFloat() /
                RenderTimings.BOX_PATH_DURATION_TICKS.toFloat()).coerceAtMost(1f)
        }
        var changed = false
        if (elapsedTicks == RenderTimings.BOX_PATH_DELAY_TICKS) {
            changed = true
        }
        if (progress != state.boxPathShrink) {
            state.boxPathShrink = progress
            changed = true
        }
        if (elapsedTicks >= RenderTimings.BOX_PATH_DELAY_TICKS +
            if (suppressLine) 0L else RenderTimings.BOX_PATH_DURATION_TICKS) {
            state.boxPathActive = false
            state.boxPathNeedsFinalClear = true
            val pending = renderStateStateful.pendingPlayerPosition
            if (pending != null) {
                renderStateStateful.displayedPlayerPosition = pending
                renderStateStateful.pendingPlayerPosition = null
            }
            renderStateStateful.pendingFacingLeft?.let { facing ->
                renderStateStateful.isFacingLeft = facing
            }
            renderStateStateful.pendingFacingLeft = null
            changed = true
        }
        return changed
    }

    private fun updateVanishAnimation(nowTick: Long): Boolean {
        val currentPosition = state.vanishPosition ?: return false
        val elapsedTicks = nowTick - state.vanishStartTick
        var cumulativeTicks = 0L

        for (step in 0..VanishSpec.LAST_STEP) {
            val delayTicks = VanishSpec.delayTicks(step)
            if (elapsedTicks < cumulativeTicks + delayTicks) {
                if (state.vanishStep != step) {
                    state.vanishStep = step
                    return true
                }
                return false
            }
            cumulativeTicks += delayTicks
        }

        if (state.vanishStep != null) {
            state.vanishStep = null
            state.vanishPosition = null
            state.vanishLastPosition = currentPosition
            state.vanishNeedsFinalClear = true
            return true
        }

        return false
    }

    private fun computeBoxPathDirtyUnionInternal(
        nowTick: Long,
        viewport: BoardViewport?
    ): Rect? {
        if (viewport == null) return null
        val boxDirty = state.boxPathDirtyRect
        val vanishTarget = if (state.vanishPosition != null || state.vanishNeedsFinalClear) {
            state.vanishPosition ?: state.vanishLastPosition
        } else {
            null
        }
        val vanishDirty = vanishTarget?.let { computeVanishDirtyRect(viewport, it) }
        var dirty = when {
            boxDirty != null && vanishDirty != null -> Rect(boxDirty).apply { union(vanishDirty) }
            boxDirty != null -> Rect(boxDirty)
            vanishDirty != null -> Rect(vanishDirty)
            else -> null
        }
        if (state.boxFlashPosition != null &&
            nowTick - state.boxFlashStartTick < RenderTimings.FLASH_DURATION_TICKS) {
            val flashRect = spriteDrawParams(viewport, state.boxFlashPosition!!, 0.90f).dirtyRect
            dirty = union(dirty, flashRect)
        }
        val silhouettePos = state.playerSilhouettePosition
        if (silhouettePos != null) {
            val silhouetteRect = spriteDrawParams(viewport, silhouettePos, 0.80f).dirtyRect
            dirty = union(dirty, silhouetteRect)
        }
        val playerFlashPos = state.playerFlashPosition
        if (playerFlashPos != null &&
            nowTick - state.playerFlashStartTick < RenderTimings.FLASH_DURATION_TICKS) {
            val flashRect = spriteDrawParams(viewport, playerFlashPos, 0.80f).dirtyRect
            dirty = union(dirty, flashRect)
        }
        return dirty
    }

    private fun computeVanishDirtyRect(viewport: BoardViewport?): Rect? {
        if (viewport == null) return null
        val position = state.vanishPosition ?: state.vanishLastPosition ?: return null
        return computeVanishDirtyRect(viewport, position)
    }

    private fun computePlayerFlashDirtyRect(viewport: BoardViewport?): Rect? {
        if (viewport == null) return null
        val position = state.playerFlashPosition ?: return null
        return spriteDrawParams(viewport, position, 0.80f).dirtyRect
    }

    private fun union(base: Rect?, extra: Rect?): Rect? {
        if (extra == null) return base
        return base?.apply { union(extra) } ?: Rect(extra)
    }
}
