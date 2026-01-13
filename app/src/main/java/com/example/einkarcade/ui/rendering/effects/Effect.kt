package com.example.einkarcade.ui.rendering.effects

import android.graphics.Canvas
import android.graphics.Rect

interface Effect {
    val durationMs: Long

    /** The region this effect affects while active, or null if none. */
    fun dirtyRect(): Rect?

    fun draw(canvas: Canvas)
}