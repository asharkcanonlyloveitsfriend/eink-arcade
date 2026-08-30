package com.example.einkarcade.ui.rendering.anim

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationRunnerTest {
    @Test
    fun clearMakesPreviouslyScheduledCallbackANoOp() {
        val scheduledCallbacks = mutableListOf<Runnable>()
        var invalidationCount = 0
        val runner =
            AnimationRunner(
                invalidateRects = { invalidationCount++ },
                postDelayed = { runnable, _ -> scheduledCallbacks += runnable },
            )

        runner.enqueue(TimedAnimation())
        val callbackFromClearedAnimation = scheduledCallbacks.single()

        runner.clear()
        runner.enqueue(TimedAnimation())

        val invalidationsBeforeStaleCallback = invalidationCount
        val scheduledBeforeStaleCallback = scheduledCallbacks.size
        callbackFromClearedAnimation.run()

        assertEquals(invalidationsBeforeStaleCallback, invalidationCount)
        assertEquals(scheduledBeforeStaleCallback, scheduledCallbacks.size)
    }

    private class TimedAnimation : Animation {
        override fun dirtyRects(): Array<Rect?> = arrayOf(Rect(0, 0, 1, 1))

        override fun ticksUntilNextStep(): Int = 1
    }
}
