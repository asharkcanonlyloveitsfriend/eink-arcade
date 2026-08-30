package com.example.einkarcade.ui.rendering.anim

import android.graphics.Canvas
import android.graphics.Rect
import java.util.ArrayDeque

/**
 * Owns animation sequencing and timing policy.
 *
 * It relies on injected callbacks for invalidation and scheduling.
 */
internal class AnimationRunner(
    private val invalidateRects: (Array<Rect?>) -> Unit,
    private val postDelayed: (Runnable, Long) -> Unit,
) {
    private val queue = ArrayDeque<Animation>()
    private var active: Animation? = null
    private var scheduleGeneration = 0

    fun enqueue(animation: Animation) {
        queue.addLast(animation)
        if (active == null) {
            startNext()
        }
    }

    fun drawUnderEntities(canvas: Canvas) {
        active?.drawUnderEntities(canvas)
    }

    fun drawOverEntities(canvas: Canvas) {
        active?.drawOverEntities(canvas)
    }

    fun hidesPlayer(): Boolean = active?.hidesPlayer() == true

    /** Stops all transient effects, including callbacks already posted for a previous board state. */
    fun clear() {
        val previous = active
        active = null
        queue.clear()
        scheduleGeneration++
        previous?.let { invalidateRects(it.dirtyRects()) }
    }

    private fun startNext() {
        val previous = active
        val next: Animation? = queue.pollFirst()

        active = null

        // Clean up previous animation region
        previous?.let { invalidateRects(it.dirtyRects()) }

        if (next == null) return

        active = next

        // Invalidate initial region if needed
        invalidateRects(next.dirtyRects())

        scheduleNextStep()
    }

    private fun scheduleNextStep() {
        val animation = active ?: return
        val ticks = animation.ticksUntilNextStep()

        if (ticks == null) {
            startNext()
        } else {
            val delayMs = ticks * ANIMATION_TICK_MS
            val generation = scheduleGeneration
            postDelayed(Runnable { advance(generation) }, delayMs)
        }
    }

    private fun advance(generation: Int) {
        if (generation != scheduleGeneration) return
        active?.let { invalidateRects(it.dirtyRects()) }
        scheduleNextStep()
    }
}
