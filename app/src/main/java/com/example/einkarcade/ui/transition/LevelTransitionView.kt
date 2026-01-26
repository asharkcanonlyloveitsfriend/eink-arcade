package com.example.einkarcade.ui.transition

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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.draw.BackgroundDrawer
import com.example.einkarcade.ui.rendering.draw.EntityDrawer
import com.example.einkarcade.ui.rendering.draw.GameRenderer
import com.example.einkarcade.ui.rendering.draw.TileDrawer
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ANIMATION_TICK_MS: Long = 50L
private const val TICKS_PER_STEP = 2
private const val STEP_PERCENT = 14 // percent of union rect width per step
private const val FLASH_GAP_STEPS = 2 // how many sweep steps to wait after the band passes a tile

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
        private val renderer =
            GameRenderer(
                assets = AndroidGameAssets(context),
                backgroundDrawer = BackgroundDrawer(context),
                tileDrawer = TileDrawer(),
                entityDrawer = EntityDrawer(AndroidGameAssets(context)),
            )

        private var oldTileMap: TileMap? = null
        private var newTileMap: TileMap? = null
        private var transitionState: TransitionState? = null
        private var stepIndex = 0
        private var hasDismissed = false

        fun setTileMaps(
            oldTileMap: TileMap,
            newTileMap: TileMap,
        ) {
            this.oldTileMap = oldTileMap
            this.newTileMap = newTileMap
            rebuildTransitionState()
            invalidate()
        }

        // Set by the host (Compose or parent view) to dismiss the overlay.
        var onDismiss: (() -> Unit)? = null

        override fun onDraw(canvas: Canvas) {
            val oldMap =
                checkNotNull(oldTileMap) { "LevelTransitionView requires oldTileMap before draw." }
            val newMap =
                checkNotNull(newTileMap) { "LevelTransitionView requires newTileMap before draw." }

            if (transitionState == null) {
                rebuildTransitionState(oldMap, newMap)
            }

            val state = transitionState
            if (state == null) {
                renderer.drawBackground(canvas, width, height)
                return
            }

            if (isDone(state)) {
                canvas.drawBitmap(state.newBitmap, 0f, 0f, null)
                if (!hasDismissed) {
                    hasDismissed = true
                    onDismiss?.invoke()
                }
                return
            }

            drawFrame(canvas, state, stepIndex)
            stepIndex++

            val back = frontS - bandS

            for (tile in state.flashTiles) {
                if (tile.phaseIndex >= TILE_FLASH_PHASES.size) continue
                if (back < tile.completionS + gapS) continue

                val phase = TILE_FLASH_PHASES[tile.phaseIndex]
                val paint =
                    when (phase) {
                        TileFlashPhaseType.BLACK -> flashBlackPaint
                        TileFlashPhaseType.WHITE -> flashWhitePaint
                        TileFlashPhaseType.NORMAL -> null
                    }

                if (paint != null) {
                    canvas.withSave {
                        clipRect(tile.rect)
                        canvas.drawRect(tile.rect, paint)
                    }
                }

                tile.phaseIndex++
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Consume touches so nothing falls through to the board.
            if (event.action == MotionEvent.ACTION_DOWN) {
                onDismiss?.invoke()
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

        private fun rebuildTransitionState() {
            val oldMap = oldTileMap ?: return
            val newMap = newTileMap ?: return
            rebuildTransitionState(oldMap, newMap)
        }

        private fun rebuildTransitionState(
            oldMap: TileMap,
            newMap: TileMap,
        ) {
            if (width <= 0 || height <= 0) return

            val oldViewport =
                computeBoardViewport(
                    surfaceWidth = width.toFloat(),
                    surfaceHeight = height.toFloat(),
                    innerRows = oldMap.rowCount,
                    innerCols = oldMap.columnCount,
                )
            val newViewport =
                computeBoardViewport(
                    surfaceWidth = width.toFloat(),
                    surfaceHeight = height.toFloat(),
                    innerRows = newMap.rowCount,
                    innerCols = newMap.columnCount,
                )

            val backgroundBitmap =
                createBitmap(width, height).also {
                    renderer.drawBackground(Canvas(it), width, height)
                }

            renderer.rebuildStaticLayout(
                viewWidth = width,
                viewHeight = height,
                viewport = newViewport,
                tileMap = newMap,
            )

            val newBitmap =
                createBitmap(width, height).also {
                    renderer.drawStaticFrame(Canvas(it))
                }

            transitionState =
                TransitionState(
                    backgroundBitmap = backgroundBitmap,
                    newBitmap = newBitmap,
                    oldViewport = oldViewport,
                    newViewport = newViewport,
                    oldTileMap = oldMap,
                    newTileMap = newMap,
                )

            stepIndex = 0
            hasDismissed = false
            invalidate()
            scheduleNextFrame()
        }

        private fun scheduleNextFrame() {
            val state = transitionState ?: return
            if (isDone(state)) {
                return
            }

            val delayMs = TICKS_PER_STEP * ANIMATION_TICK_MS
            postDelayed(
                {
                    invalidate()
                    scheduleNextFrame()
                },
                delayMs,
            )
        }

        private fun isDone(state: TransitionState): Boolean = state.flashTiles.all { it.phaseIndex >= TILE_FLASH_PHASES.size }

        private data class FlashTile(
            val rect: Rect,
            val completionS: Float,
            var phaseIndex: Int = 0,
        )

        private data class TransitionState(
            val backgroundBitmap: Bitmap,
            val newBitmap: Bitmap,
            val oldViewport: BoardViewport,
            val newViewport: BoardViewport,
            val oldTileMap: TileMap,
            val newTileMap: TileMap,
        ) {
            private val oldBoardRect: Rect =
                Rect(
                    (oldViewport.offsetX + 1f * oldViewport.cellSize).roundToInt(),
                    (oldViewport.offsetY + 1f * oldViewport.cellSize).roundToInt(),
                    (oldViewport.offsetX + (oldViewport.cols - 1) * oldViewport.cellSize).roundToInt(),
                    (oldViewport.offsetY + (oldViewport.rows - 1) * oldViewport.cellSize).roundToInt(),
                )

            private val newBoardRect: Rect =
                Rect(
                    (newViewport.offsetX + 1f * newViewport.cellSize).roundToInt(),
                    (newViewport.offsetY + 1f * newViewport.cellSize).roundToInt(),
                    (newViewport.offsetX + (newViewport.cols - 1) * newViewport.cellSize).roundToInt(),
                    (newViewport.offsetY + (newViewport.rows - 1) * newViewport.cellSize).roundToInt(),
                )

            val unionBoardRect: Rect =
                Rect().apply {
                    set(oldBoardRect)
                    union(newBoardRect)
                }

            private val boardWidth = unionBoardRect.width().toFloat()
            private val boardHeight = unionBoardRect.height().toFloat()
            private val boardLeft = unionBoardRect.left.toFloat()
            private val boardBottom = unionBoardRect.bottom.toFloat()

            private fun interiorBoardRect(viewport: BoardViewport): Rect {
                val left = (viewport.offsetX + 1f * viewport.cellSize).roundToInt()
                val top = (viewport.offsetY + 1f * viewport.cellSize).roundToInt()
                val right = (viewport.offsetX + (viewport.cols - 1) * viewport.cellSize).roundToInt()
                val bottom = (viewport.offsetY + (viewport.rows - 1) * viewport.cellSize).roundToInt()
                return Rect(left, top, right, bottom)
            }

            private val oldInteriorRect by lazy { interiorBoardRect(oldViewport) }
            private val newInteriorRect by lazy { interiorBoardRect(newViewport) }

            private fun computeWallRegion(
                viewport: BoardViewport,
                tileMap: TileMap,
                interiorRect: Rect,
            ): Region {
                val region = Region()

                region.op(unionBoardRect, Region.Op.UNION)
                region.op(interiorRect, Region.Op.DIFFERENCE)

                for (r in 0 until tileMap.rowCount) {
                    for (c in 0 until tileMap.columnCount) {
                        if (tileMap.isVoid(r, c)) {
                            val left = (viewport.offsetX + (c + 1) * viewport.cellSize).roundToInt()
                            val top = (viewport.offsetY + (r + 1) * viewport.cellSize).roundToInt()
                            val right = (viewport.offsetX + (c + 2) * viewport.cellSize).roundToInt()
                            val bottom = (viewport.offsetY + (r + 2) * viewport.cellSize).roundToInt()
                            region.op(
                                Rect(left, top, right, bottom),
                                Region.Op.UNION,
                            )
                        }
                    }
                }

                return region
            }

            private val stableWallRegion: Region by lazy {
                val oldWalls = computeWallRegion(oldViewport, oldTileMap, oldInteriorRect)
                val newWalls = computeWallRegion(newViewport, newTileMap, newInteriorRect)
                oldWalls.apply { op(newWalls, Region.Op.INTERSECT) }
            }

            val stableWallRects: List<Rect> by lazy {
                val out = mutableListOf<Rect>()
                val it = RegionIterator(stableWallRegion)
                val r = Rect()
                while (it.next(r)) out.add(Rect(r))
                out
            }

            private fun sFor(
                x: Float,
                y: Float,
            ): Float = (x - boardLeft) / boardWidth + (boardBottom - y) / boardHeight

            val flashTiles: List<FlashTile> by lazy {
                val out = mutableListOf<FlashTile>()

                for (r in 0 until newTileMap.rowCount) {
                    for (c in 0 until newTileMap.columnCount) {
                        val left = newViewport.offsetX + (c + 1) * newViewport.cellSize
                        val top = newViewport.offsetY + (r + 1) * newViewport.cellSize
                        val right = newViewport.offsetX + (c + 2) * newViewport.cellSize
                        val bottom = newViewport.offsetY + (r + 2) * newViewport.cellSize

                        val rect =
                            Rect(
                                left.roundToInt(),
                                top.roundToInt(),
                                right.roundToInt(),
                                bottom.roundToInt(),
                            )

                        val completionS =
                            max(
                                sFor(left, top),
                                max(
                                    sFor(right, top),
                                    max(sFor(left, bottom), sFor(right, bottom)),
                                ),
                            )

                        out.add(FlashTile(rect, completionS))
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

        private val stepS = (STEP_PERCENT / 100f) * 2f
        private val bandS = 3f * stepS
        private val gapS = FLASH_GAP_STEPS * stepS
        private val frontS: Float
            get() = stepIndex * stepS

        private fun drawFrame(
            canvas: Canvas,
            state: TransitionState,
            stepIndex: Int,
        ) {
            val frontS = stepIndex * stepS

            canvas.drawBitmap(state.backgroundBitmap, 0f, 0f, null)

            val k0 = frontS - bandS
            if (k0 > -1000f) {
                drawSBand(canvas, state, -1000f, k0, state.newBitmap, null)
            }

            drawSBand(canvas, state, frontS - bandS, frontS - 2f * stepS, state.newBitmap, invertPaint)
            drawSBand(canvas, state, frontS - 2f * stepS, frontS - stepS, state.newBitmap, null)
            drawSBand(canvas, state, frontS - stepS, frontS, state.backgroundBitmap, invertPaint)
        }

        private fun drawSBand(
            canvas: Canvas,
            state: TransitionState,
            a: Float,
            b: Float,
            bitmap: Bitmap,
            paint: Paint?,
        ) {
            val lo = min(a, b)
            val hi = max(a, b)

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

                val yTop0 = yForS(hi, x, boardWidth, boardHeight, boardLeft, boardBottom)
                val yTop1 = yForS(hi, x2, boardWidth, boardHeight, boardLeft, boardBottom)
                val yBot0 = yForS(lo, x, boardWidth, boardHeight, boardLeft, boardBottom)
                val yBot1 = yForS(lo, x2, boardWidth, boardHeight, boardLeft, boardBottom)

                val top = min(yTop0, yTop1).coerceIn(topBound, bottomBound)
                val bottom = max(yBot0, yBot1).coerceIn(topBound, bottomBound)

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
                        for (r in state.stableWallRects) {
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
