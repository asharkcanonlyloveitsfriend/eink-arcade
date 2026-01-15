package com.example.einkarcade.ui.rendering.anim

import android.graphics.Canvas
import android.graphics.Rect

const val ANIMATION_TICK_MS: Long = 50L

interface Animation {
    /** Region affected at the current state, or null if none. */
    fun dirtyRect(): Rect?

    /** Draw in the current state. */
    fun draw(canvas: Canvas)

    /**
     * Number of animation ticks until the next state change.
     * Return null when the animation is complete.
     */
    fun ticksUntilNextStep(): Int?
}