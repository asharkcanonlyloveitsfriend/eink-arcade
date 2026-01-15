package com.example.einkarcade.ui.rendering.anim

interface SurfaceAnimation {
    /**
     * Advance the animation by one tick.
     *
     * @return true if the animation should continue,
     *         false if it is complete and should be removed.
     */
    fun tick(): Boolean
}