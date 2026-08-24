package com.example.einkarcade.ui.modes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil

class LevelSolvedOverlay
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        var onAdvance: (() -> Unit)? = null
        var boxMoveCount: Int = 0
            set(value) {
                field = value
                refreshContent()
            }
        var isNewBestSolution: Boolean = false
            set(value) {
                field = value
                refreshContent()
            }

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

        private val moveCountPaint =
            Paint(textPaint).apply {
                textSize = 20f * resources.displayMetrics.density * resources.configuration.fontScale
            }

        init {
            setOnClickListener { onAdvance?.invoke() }
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val density = resources.displayMetrics.density
            val horizontalPadding = HORIZONTAL_PADDING_DP * density
            val verticalPadding = VERTICAL_PADDING_DP * density
            val titleHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
            val moveCountHeight = moveCountPaint.fontMetrics.descent - moveCountPaint.fontMetrics.ascent
            val lineSpacing = LINE_SPACING_DP * density
            val textWidth = maxOf(textPaint.measureText(TITLE), moveCountPaint.measureText(moveCountMessage()))

            val desiredWidth = ceil(textWidth + horizontalPadding * 2f).toInt()
            val desiredHeight = ceil(titleHeight + lineSpacing + moveCountHeight + verticalPadding * 2).toInt()
            setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val density = resources.displayMetrics.density

            val title = TITLE
            val moveCount = moveCountMessage()
            val titleMetrics = textPaint.fontMetrics
            val moveCountMetrics = moveCountPaint.fontMetrics
            val titleHeight = titleMetrics.descent - titleMetrics.ascent
            val moveCountHeight = moveCountMetrics.descent - moveCountMetrics.ascent
            val lineSpacing = LINE_SPACING_DP * density
            val textHeight = titleHeight + lineSpacing + moveCountHeight

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
            val borderInset = borderPaint.strokeWidth / 2f
            canvas.drawRect(
                borderInset,
                borderInset,
                width - borderInset,
                height - borderInset,
                borderPaint,
            )

            val textX = width / 2f
            val titleY = height / 2f - textHeight / 2f - titleMetrics.ascent
            val moveCountY = titleY + titleMetrics.descent + lineSpacing - moveCountMetrics.ascent
            canvas.drawText(title, textX, titleY, textPaint)
            canvas.drawText(moveCount, textX, moveCountY, moveCountPaint)
        }

        private fun refreshContent() {
            updateContentDescription()
            requestLayout()
            invalidate()
        }

        private fun updateContentDescription() {
            contentDescription = "You win! ${moveCountMessage()}"
        }

        private fun moveCountMessage(): String =
            "Moves: $boxMoveCount${if (isNewBestSolution) "!" else ""}"

        private companion object {
            const val TITLE = "You win!"
            const val HORIZONTAL_PADDING_DP = 24f
            const val VERTICAL_PADDING_DP = 16f
            const val LINE_SPACING_DP = 4f
        }
    }
