package com.example.einkarcade.ui.rendering.effects

import android.graphics.Canvas
import android.graphics.Rect

class WaitEffect(
    override val durationMs: Long
) : Effect {
    override fun dirtyRect(): Rect? = null
    override fun draw(canvas: Canvas) = Unit
}