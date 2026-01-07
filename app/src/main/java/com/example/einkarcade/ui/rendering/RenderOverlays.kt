package com.example.einkarcade.ui.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.withTranslation

internal const val FLASH_PHASE_MS: Long = 50L

internal fun drawSprite(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    sizePx: Int,
    flipX: Boolean,
    paint: Paint
) {
    canvas.withTranslation(left, top) {
        if (flipX) {
            scale(-1f, 1f, sizePx / 2f, sizePx / 2f)
        }
        drawBitmap(bitmap, 0f, 0f, paint)
    }
}

internal fun drawFlashedSprite(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    sizePx: Int,
    flipX: Boolean,
    elapsedMs: Long,
    darkPaint: Paint,
    lightPaint: Paint
) {
    val paint = if (elapsedMs <= FLASH_PHASE_MS) darkPaint else lightPaint
    drawSprite(canvas, bitmap, left, top, sizePx, flipX, paint)
}

internal fun drawFlashedBitmap(
    canvas: Canvas,
    bitmap: Bitmap,
    left: Float,
    top: Float,
    elapsedMs: Long,
    darkPaint: Paint,
    lightPaint: Paint
) {
    val paint = if (elapsedMs <= FLASH_PHASE_MS) darkPaint else lightPaint
    canvas.drawBitmap(bitmap, left, top, paint)
}
