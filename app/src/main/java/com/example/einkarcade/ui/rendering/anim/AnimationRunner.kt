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
    private var generation: Int = 0

    fun enqueue(animation: Animation) {
        queue.addLast(animation)
        if (active == null) {
            startNext()
        }
    }

    fun replaceQueue(animation: Animation) {
        generation++
        queue.clear()
        queue.addLast(animation)
        startNext()
    }

    fun drawUnderEntities(canvas: Canvas) {
        active?.drawUnderEntities(canvas)
    }

    fun drawOverEntities(canvas: Canvas) {
        active?.drawOverEntities(canvas)
    }

    fun hidesPlayer(): Boolean = active?.hidesPlayer() == true

    fun hidesBoard(): Boolean = active?.hidesBoard() == true

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
            val scheduledGeneration = generation
            val delayMs = ticks * ANIMATION_TICK_MS
            postDelayed(Runnable { advance(scheduledGeneration) }, delayMs)
        }
    }

    private fun advance(scheduledGeneration: Int) {
        if (generation != scheduledGeneration) return

        active?.let { invalidateRects(it.dirtyRects()) }
        scheduleNextStep()
    }
}
