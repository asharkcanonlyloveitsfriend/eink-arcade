package com.example.einkarcade.ui.modes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.graphics.RegionIterator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withSave
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.rendering.StaticBoardFrame
import com.example.einkarcade.ui.rendering.draw.BackgroundBitmapCache
import com.example.einkarcade.ui.rendering.draw.BackgroundDrawer
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import kotlin.math.min
import kotlin.math.roundToInt

private const val ANIMATION_STEP_MS = 100L
private const val STEP_PERCENT = 14 // percent of union rect width per step
private const val FLASH_GAP_STEPS = 2 // how many sweep steps to wait after the band passes a tile
private const val STEP_S = (STEP_PERCENT / 100f) * 2f
private const val BAND_S = 3f * STEP_S
private const val FLASH_GAP_S = FLASH_GAP_STEPS * STEP_S

private enum class TileFlashPhaseType {
    BLACK,
    NORMAL,
    WHITE,
}

private val TILE_FLASH_PHASES =
    listOf(
        TileFlashPhaseType.BLACK,
        TileFlashPhaseType.NORMAL,
        TileFlashPhaseType.WHITE,
        TileFlashPhaseType.NORMAL,
        TileFlashPhaseType.BLACK,
    )

class LevelTransitionView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val backgroundDrawer = BackgroundDrawer(context)

        private var transitionData: TransitionData? = null
        private var transitionState: TransitionState? = null
        private var stepIndex = 0
        private var hasDismissed = false

        private val advanceAnimation =
            Runnable {
                val state = transitionState ?: return@Runnable
                if (isDone(state)) return@Runnable

                stepIndex++
                invalidate()
                scheduleNextFrame()
            }

        internal fun setTransitionData(
            oldViewport: BoardViewport,
            oldTileMap: TileMap,
            newFrame: StaticBoardFrame,
        ) {
            removeCallbacks(advanceAnimation)
            transitionData = TransitionData(oldViewport, oldTileMap, newFrame)
            transitionState = null
            rebuildTransitionState()
        }

        // Set by the host (Compose or parent view) to dismiss the view.
        var onDismiss: (() -> Unit)? = null

        override fun onDraw(canvas: Canvas) {
            if (transitionState == null) {
                rebuildTransitionState()
            }

            val state = transitionState
            if (state == null) {
                backgroundDrawer.draw(canvas, width, height)
                return
            }

            if (isDone(state)) {
                canvas.drawBitmap(state.newBitmap, 0f, 0f, null)
                dismissOnce()
                return
            }

            drawFrame(canvas, state, stepIndex)

            for (tile in state.flashTiles) {
                val phaseIndex = flashPhaseIndex(tile) ?: continue
                val phase = TILE_FLASH_PHASES.getOrNull(phaseIndex) ?: continue
                val paint =
                    when (phase) {
                        TileFlashPhaseType.BLACK -> flashBlackPaint
                        TileFlashPhaseType.WHITE -> flashWhitePaint
                        TileFlashPhaseType.NORMAL -> null
                    }

                if (paint != null) {
                    canvas.drawRect(tile.rect, paint)
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Consume touches so nothing falls through to the board.
            if (event.action == MotionEvent.ACTION_DOWN) {
                dismissOnce()
            }
            return true
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            rebuildTransitionState()
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(advanceAnimation)
            super.onDetachedFromWindow()
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            scheduleNextFrame()
        }

        private fun rebuildTransitionState() {
            val data = transitionData ?: return
            if (width <= 0 || height <= 0) return

            removeCallbacks(advanceAnimation)
            val backgroundBitmap =
                BackgroundBitmapCache.get(
                    context = context,
                    width = width,
                    height = height,
                )

            transitionState =
                TransitionState(
                    backgroundBitmap = backgroundBitmap,
                    newBitmap = data.newFrame.bitmap,
                    oldViewport = data.oldViewport,
                    newViewport = data.newFrame.viewport,
                    oldTileMap = data.oldTileMap,
                    newTileMap = data.newFrame.tileMap,
                )

            stepIndex = 0
            hasDismissed = false
            invalidate()
            scheduleNextFrame()
        }

        private fun scheduleNextFrame() {
            val state = transitionState ?: return
            if (isDone(state) || hasDismissed) return

            removeCallbacks(advanceAnimation)
            postDelayed(advanceAnimation, ANIMATION_STEP_MS)
        }

        private fun flashPhaseIndex(tile: FlashTile): Int? =
            computeFlashPhaseIndex(
                stepIndex = stepIndex,
                firstFlashStep = tile.firstFlashStep,
            )

        private fun isDone(state: TransitionState): Boolean =
            state.flashTiles.all { tile ->
                (flashPhaseIndex(tile) ?: -1) >= TILE_FLASH_PHASES.size
            }

        private fun dismissOnce() {
            if (hasDismissed) return
            hasDismissed = true
            removeCallbacks(advanceAnimation)
            onDismiss?.invoke()
        }

        private data class TransitionData(
            val oldViewport: BoardViewport,
            val oldTileMap: TileMap,
            val newFrame: StaticBoardFrame,
        )

        private data class FlashTile(
            val rect: Rect,
            val firstFlashStep: Int,
        )

        private class TransitionState(
            val backgroundBitmap: Bitmap,
            val newBitmap: Bitmap,
            oldViewport: BoardViewport,
            newViewport: BoardViewport,
            oldTileMap: TileMap,
            newTileMap: TileMap,
        ) {
            private val oldBoardRect = oldViewport.toBoardRect()
            private val newBoardRect = newViewport.toBoardRect()

            val unionBoardRect: Rect =
                Rect().apply {
                    set(oldBoardRect)
                    union(newBoardRect)
                }

            private val boardWidth = unionBoardRect.width().toFloat()
            private val boardHeight = unionBoardRect.height().toFloat()
            private val boardLeft = unionBoardRect.left.toFloat()
            private val boardBottom = unionBoardRect.bottom.toFloat()

            private fun computeVoidRegion(
                viewport: BoardViewport,
                tileMap: TileMap,
                boardRect: Rect,
            ): Region {
                val region = Region()

                region.op(unionBoardRect, Region.Op.UNION)
                region.op(boardRect, Region.Op.DIFFERENCE)

                for (r in 0 until tileMap.rowCount) {
                    for (c in 0 until tileMap.columnCount) {
                        if (tileMap.isVoid(r, c)) {
                            val left = viewport.cellLeft(c).roundToInt()
                            val top = viewport.cellTop(r).roundToInt()
                            val right = viewport.cellLeft(c + 1).roundToInt()
                            val bottom = viewport.cellTop(r + 1).roundToInt()
                            region.op(
                                Rect(left, top, right, bottom),
                                Region.Op.UNION,
                            )
                        }
                    }
                }

                return region
            }

            val stableVoidRects: List<Rect> =
                run {
                    val oldVoids = computeVoidRegion(oldViewport, oldTileMap, oldBoardRect)
                    val newVoids = computeVoidRegion(newViewport, newTileMap, newBoardRect)
                    val stableVoids = oldVoids.apply { op(newVoids, Region.Op.INTERSECT) }

                    val out = mutableListOf<Rect>()
                    val iterator = RegionIterator(stableVoids)
                    val r = Rect()
                    while (iterator.next(r)) out.add(Rect(r))
                    out
                }

            private fun sFor(
                x: Float,
                y: Float,
            ): Float = (x - boardLeft) / boardWidth + (boardBottom - y) / boardHeight

            val flashTiles: List<FlashTile> =
                run {
                    val out = mutableListOf<FlashTile>()

                    for (r in 0 until newTileMap.rowCount) {
                        for (c in 0 until newTileMap.columnCount) {
                            val left = newViewport.cellLeft(c)
                            val top = newViewport.cellTop(r)
                            val right = newViewport.cellLeft(c + 1)
                            val bottom = newViewport.cellTop(r + 1)

                            val rect =
                                Rect(
                                    left.roundToInt(),
                                    top.roundToInt(),
                                    right.roundToInt(),
                                    bottom.roundToInt(),
                                )

                            val completionS = sFor(right, top)
                            val firstFlashStep =
                                computeFirstFlashStep(
                                    completionS = completionS,
                                    stepS = STEP_S,
                                    bandS = BAND_S,
                                    gapS = FLASH_GAP_S,
                                )

                            out.add(FlashTile(rect, firstFlashStep))
                        }
                    }
                    out
                }
        }

        private val invertPaint =
            Paint().apply {
                colorFilter =
                    ColorMatrixColorFilter(
                        android.graphics.ColorMatrix(
                            floatArrayOf(
                                -1f,
                                0f,
                                0f,
                                0f,
                                255f,
                                0f,
                                -1f,
                                0f,
                                0f,
                                255f,
                                0f,
                                0f,
                                -1f,
                                0f,
                                255f,
                                0f,
                                0f,
                                0f,
                                1f,
                                0f,
                            ),
                        ),
                    )
                isAntiAlias = false
            }

        private val flashBlackPaint =
            Paint().apply {
                color = android.graphics.Color.BLACK
                isAntiAlias = false
            }

        private val flashWhitePaint =
            Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = false
            }

        private fun drawFrame(
            canvas: Canvas,
            state: TransitionState,
            stepIndex: Int,
        ) {
            val frontS = stepIndex * STEP_S

            canvas.drawBitmap(state.backgroundBitmap, 0f, 0f, null)

            val k0 = frontS - BAND_S
            if (k0 > 0f) {
                drawSBand(canvas, state, 0f, k0, state.newBitmap, null)
            }

            drawSBand(canvas, state, frontS - BAND_S, frontS - 2f * STEP_S, state.newBitmap, invertPaint)
            drawSBand(canvas, state, frontS - 2f * STEP_S, frontS - STEP_S, state.newBitmap, null)
            drawSBand(canvas, state, frontS - STEP_S, frontS, state.backgroundBitmap, invertPaint)
        }

        private fun drawSBand(
            canvas: Canvas,
            state: TransitionState,
            fromS: Float,
            toS: Float,
            bitmap: Bitmap,
            paint: Paint?,
        ) {
            val unionBoardRect = state.unionBoardRect
            val boardWidth = unionBoardRect.width().toFloat()
            val boardHeight = unionBoardRect.height().toFloat()
            val boardLeft = unionBoardRect.left.toFloat()
            val boardBottom = unionBoardRect.bottom.toFloat()

            val left = unionBoardRect.left
            val right = unionBoardRect.right
            val topBound = unionBoardRect.top.toFloat()
            val bottomBound = unionBoardRect.bottom.toFloat()

            val sliceWidthPx = boardWidth * (STEP_PERCENT / 100f)
            var x = left.toFloat()
            while (x < right) {
                val x2 = min(x + sliceWidthPx, right.toFloat())

                val top =
                    yForS(toS, x, boardWidth, boardHeight, boardLeft, boardBottom)
                        .coerceIn(topBound, bottomBound)
                val bottom =
                    yForS(fromS, x2, boardWidth, boardHeight, boardLeft, boardBottom)
                        .coerceIn(topBound, bottomBound)

                if (top < bottom) {
                    val sliceRect =
                        Rect(
                            x.roundToInt(),
                            top.roundToInt(),
                            x2.roundToInt(),
                            bottom.roundToInt(),
                        )
                    canvas.withSave {
                        clipRect(unionBoardRect)
                        clipRect(sliceRect)
                        for (r in state.stableVoidRects) {
                            canvas.clipOutRect(r)
                        }
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    }
                }

                x = x2
            }
        }

        private fun yForS(
            k: Float,
            x: Float,
            boardWidth: Float,
            boardHeight: Float,
            boardLeft: Float,
            boardBottom: Float,
        ): Float =
            boardBottom -
                boardHeight *
                (k - (x - boardLeft) / boardWidth)
    }

private fun BoardViewport.toBoardRect(): Rect =
    Rect(
        boardLeft.roundToInt(),
        boardTop.roundToInt(),
        boardRight.roundToInt(),
        boardBottom.roundToInt(),
    )

internal fun computeFirstFlashStep(
    completionS: Float,
    stepS: Float,
    bandS: Float,
    gapS: Float,
): Int {
    var stepIndex = 0
    while ((stepIndex + 1) * stepS - bandS < completionS + gapS) {
        stepIndex++
    }
    return stepIndex
}

internal fun computeFlashPhaseIndex(
    stepIndex: Int,
    firstFlashStep: Int,
): Int? = (stepIndex - firstFlashStep).takeIf { it >= 0 }
