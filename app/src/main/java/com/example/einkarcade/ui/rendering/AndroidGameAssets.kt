package com.example.einkarcade.ui.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.content.res.AppCompatResources

internal class AndroidGameAssets(private val context: Context) {
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
    }

    private val cache = mutableMapOf<Int, MutableMap<Int, Bitmap>>()

    fun bitmapPaint(): Paint = bitmapPaint

    fun getBitmap(resId: Int, sizePx: Int): Bitmap {
        require(sizePx > 0)
        val bySize = cache.getOrPut(resId) { mutableMapOf() }
        return bySize.getOrPut(sizePx) {
            val drawable = AppCompatResources.getDrawable(context, resId)
            requireNotNull(drawable) { "Missing drawable: $resId" }
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        }
    }
}
