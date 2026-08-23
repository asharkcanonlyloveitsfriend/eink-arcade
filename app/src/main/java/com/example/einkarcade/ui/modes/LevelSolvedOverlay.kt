package com.example.einkarcade.ui.modes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class LevelSolvedOverlay
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        var onAdvance: (() -> Unit)? = null
        private val boxRect = RectF()

        private val borderPaint =
            Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f * resources.displayMetrics.density
                isAntiAlias = false
            }

        private val fillPaint =
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = false
            }

        private val textPaint =
            Paint().apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                val density = resources.displayMetrics.density
                val fontScale = resources.configuration.fontScale
                textSize = 32f * density * fontScale
                isAntiAlias = true
            }

        override fun onDraw(canvas: Canvas) {
            val density = resources.displayMetrics.density

            val text = "You win!"
            val textWidth = textPaint.measureText(text)
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent

            val horizontalPadding = 24f * density
            val verticalPadding = 16f * density

            val boxWidth = textWidth + horizontalPadding * 2
            val boxHeight = textHeight + verticalPadding * 2

            val left = (width - boxWidth) / 2f
            val top = (height - boxHeight) / 2f
            boxRect.set(
                left,
                top,
                left + boxWidth,
                top + boxHeight,
            )
            val box = boxRect

            canvas.drawRect(box, fillPaint)
            canvas.drawRect(box, borderPaint)

            val textX = box.centerX()
            val textY = box.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2f
            canvas.drawText(text, textX, textY, textPaint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.x > width / 2f) {
                    onAdvance?.invoke()
                }
                return true
            }
            return super.onTouchEvent(event)
        }
    }
