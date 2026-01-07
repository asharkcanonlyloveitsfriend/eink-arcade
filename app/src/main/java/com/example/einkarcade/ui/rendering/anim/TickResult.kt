package com.example.einkarcade.ui.rendering.anim

import android.graphics.Rect

internal data class TickResult(
    val dirtyRect: Rect?,
    val needsNextFrame: Boolean,
    val forceFullRender: Boolean,
    val nextFrameDelayMs: Long? = null
)
