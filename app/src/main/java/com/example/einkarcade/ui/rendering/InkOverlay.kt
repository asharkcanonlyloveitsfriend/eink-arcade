package com.example.einkarcade.ui.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent

internal class InkOverlay(
    density: Float,
    private val invalidate: (Int, Int, Int, Int) -> Unit,
) {
    private val paint: Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(120, 0, 0, 0)
        }

    private val radiusFraction = 0.06f

    // Only begin drawing once movement exceeds this threshold.
    // This must be large enough to tolerate normal tap jitter.
    private val drawActivationThresholdPx: Float = 22f * density

    private var active = false
    private var hasDown = false

    private var x = 0f
    private var y = 0f
    private var prevX = 0f
    private var prevY = 0f
    private var downX = 0f
    private var downY = 0f

    // Becomes true once we have activated ink drawing (and therefore should NOT treat ACTION_UP as a tap).
    private var hasDragged = false

    private var radiusPx = 0f

    fun onSizeChanged(width: Int) {
        radiusPx = width * radiusFraction
    }

    fun onTouchEvent(
        event: MotionEvent,
        onTap: ((Float, Float) -> Unit)? = null,
    ): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Do not activate ink on down; only after movement exceeds a threshold.
                hasDown = true
                active = false
                hasDragged = false

                x = event.x
                y = event.y
                prevX = x
                prevY = y

                downX = x
                downY = y

                // No invalidation here: taps should not show ink.
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // If we missed ACTION_DOWN, initialize state from the first MOVE.
                if (!hasDown) {
                    hasDown = true
                    active = false
                    hasDragged = false

                    x = event.x
                    y = event.y
                    prevX = x
                    prevY = y

                    downX = x
                    downY = y
                    return true
                }

                prevX = x
                prevY = y

                x = event.x
                y = event.y

                val dxFromDown = x - downX
                val dyFromDown = y - downY
                val dist2FromDown = dxFromDown * dxFromDown + dyFromDown * dyFromDown

                val activate2 = drawActivationThresholdPx * drawActivationThresholdPx
                if (!active && dist2FromDown >= activate2) {
                    // Start drawing only after a meaningful movement.
                    active = true
                    hasDragged = true
                }

                if (active) {
                    val r = radiusPx + 2f
                    val left = kotlin.math.min(prevX, x) - r
                    val top = kotlin.math.min(prevY, y) - r
                    val right = kotlin.math.max(prevX, x) + r
                    val bottom = kotlin.math.max(prevY, y) + r
                    invalidate(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                // Only treat as a tap if we never activated ink drawing.
                if (!hasDragged) {
                    onTap?.invoke(event.x, event.y)
                }

                val oldX = x
                val oldY = y
                val wasActive = active

                hasDown = false
                active = false
                hasDragged = false

                if (wasActive) {
                    invalidateCircle(oldX, oldY)
                }

                // Reset coordinates for cleanliness (not used for missed-DOWN logic anymore).
                downX = 0f
                downY = 0f
                prevX = 0f
                prevY = 0f
                x = 0f
                y = 0f

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val oldX = x
                val oldY = y
                val wasActive = active

                hasDown = false
                active = false
                hasDragged = false

                if (wasActive) {
                    invalidateCircle(oldX, oldY)
                }

                downX = 0f
                downY = 0f
                prevX = 0f
                prevY = 0f
                x = 0f
                y = 0f

                return true
            }

            else -> {
                return true
            }
        }
    }

    fun draw(canvas: Canvas) {
        if (!active) return
        if (radiusPx <= 0f) return
        canvas.drawCircle(x, y, radiusPx, paint)
    }

    private fun invalidateCircle(
        centerX: Float,
        centerY: Float,
    ) {
        val r = radiusPx + 2f
        invalidate(
            (centerX - r).toInt(),
            (centerY - r).toInt(),
            (centerX + r).toInt(),
            (centerY + r).toInt(),
        )
    }
}
