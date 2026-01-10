package com.example.einkarcade.ui.rendering.anim

import android.graphics.Rect

class BlinkAnimation(
    private val delayTicks: Long,
    private val blinkTicks: Long,
    private val dirtyRect: Rect,
    private val renderBlinkDirty: (Rect, Boolean) -> Unit
) : Animation {

    private var tick: Long = 0L

    override fun tick(): Boolean {
        when {
            tick < delayTicks -> {
                // do nothing
            }

            tick < delayTicks + blinkTicks -> {
                renderBlinkDirty(dirtyRect, true)
            }

            tick == delayTicks + blinkTicks -> {
                renderBlinkDirty(dirtyRect, false)
                return false
            }
        }

        tick++
        return true
    }
}
