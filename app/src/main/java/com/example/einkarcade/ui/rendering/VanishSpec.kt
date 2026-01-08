package com.example.einkarcade.ui.rendering

/**
 * SurfaceView-only vanish timing + geometry.
 *
 * Steps are 0..LAST_STEP inclusive.
 */
internal object VanishSpec {
    const val LAST_STEP: Int = 6
    const val TOTAL_STEPS: Int = LAST_STEP + 1

    private val STEP_DELAYS_TICKS = longArrayOf(6L, 4L, 3L, 2L, 2L, 1L, 1L)

    fun delayTicks(step: Int): Long {
        return STEP_DELAYS_TICKS.getOrElse(step) { 0L }
    }

    fun totalDurationTicks(): Long {
        var total = 0L
        for (step in 0..LAST_STEP) {
            total += delayTicks(step)
        }
        return total
    }

    fun scale(step: Int): Float = when (step) {
        0 -> 1.0f
        1 -> 0.75f
        2 -> 0.5f
        3 -> 0.3f
        4 -> 0.18f
        5 -> 0.14f
        6 -> 0.1f
        else -> error("Unsupported vanish step: $step")
    }
}
