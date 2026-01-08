package com.example.einkarcade.ui.rendering

internal object RenderTimings {
    const val TICK_MS: Long = 50L

    const val BOX_PATH_DURATION_TICKS: Long = 5L
    const val BOX_PATH_DELAY_TICKS: Long = 2L
    const val FLASH_DURATION_TICKS: Long = 2L
    const val FLASH_PHASE_TICKS: Long = 1L
    const val BLINK_DELAY_TICKS: Long = 8L
    const val BLINK_DURATION_TICKS: Long = 6L

    fun nowTick(nowMs: Long): Long = nowMs / TICK_MS

    fun ticksToMs(ticks: Long): Long = ticks * TICK_MS

    fun msUntilNextTick(nowMs: Long): Long {
        val rem = nowMs % TICK_MS
        return if (rem == 0L) 0L else (TICK_MS - rem)
    }

    fun msUntilTick(targetTick: Long, nowMs: Long): Long {
        val targetMs = ticksToMs(targetTick)
        return (targetMs - nowMs).coerceAtLeast(0L)
    }
}
