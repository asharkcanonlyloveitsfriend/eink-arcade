package com.example.einkarcade.ui.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import com.example.einkarcade.GameController
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import com.example.einkarcade.ui.rendering.geom.computeBoxPathDirtyRect
import com.example.einkarcade.ui.rendering.geom.computeVanishDirtyRect
import com.example.einkarcade.ui.rendering.geom.screenToInnerCell
import com.example.einkarcade.ui.rendering.geom.snapToWholePixel
import com.example.einkarcade.ui.rendering.geom.spriteDrawParams
import com.example.einkarcade.ui.rendering.geom.toRenderPoint
import com.example.einkarcade.ui.rendering.model.AnimationState
import com.example.einkarcade.ui.rendering.model.LevelInit
import com.example.einkarcade.ui.rendering.model.RenderState
import com.example.einkarcade.ui.rendering.model.TransitionState
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

@SuppressLint("ClickableViewAccessibility")
internal class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val renderState = RenderState()
    private val animationState = AnimationState()
    private val transitionState = TransitionState()
    private var onTapCell: ((Position) -> Unit)? = null
    private var lastViewport: BoardViewport? = null
    private val assets = AndroidGameAssets(context)
    private var backgroundBitmap: Bitmap? = null
    private var staticFrameBitmap: Bitmap? = null
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val backgroundSrcRect = Rect()
    private val backgroundDstRect = Rect()
    private val floorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val floorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF0F0F0.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val goalFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE0E0E0.toInt() }
    private val goalStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val boxPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD3D3D3.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val boxPathTailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF2F2F2.toInt()
        style = Paint.Style.FILL
    }
    private val playerSilhouetteDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFF8E8E8E.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val playerSilhouetteLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFFF2F2F2.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val playerFlashDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFF8E8E8E.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val playerFlashLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFFF2F2F2.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val boxFlashDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFF8E8E8E.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val boxFlashLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(0xFFF2F2F2.toInt(), PorterDuff.Mode.SRC_IN)
    }
    private val boxPathDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD3D3D3.toInt()
        style = Paint.Style.FILL
    }
    private val blinkStartRunnable = Runnable {
        renderBlinkDirty()
        postOnAnimation(animationFrameRunnable)
    }
    private val animationFrameRunnable = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val changed = updateBoxPathAnimation(now)
            val vanishChanged = updateVanishAnimation(now)
            val transitionActive = transitionState.transition?.let { !it.isComplete(now) } == true
            val blinkActive = isBlinking(now)
            val pendingBlink = !blinkActive && animationState.blinkStartMs > now
            val playerFlashActive =
                animationState.playerFlashPosition != null &&
                    (now - animationState.playerFlashStartMs) <= RenderTimings.FLASH_DURATION_MS
            if (blinkActive != animationState.lastBlinkActive) {
                animationState.lastBlinkActive = blinkActive
                if (!changed && !vanishChanged) {
                    if (animationState.boxPathActive || animationState.boxPathNeedsFinalClear) {
                        renderBoxPathOrFull()
                    } else {
                        renderBlinkDirty()
                    }
                }
            }
            if (changed) {
                renderBoxPathOrFull()
            }
            if (vanishChanged && !changed) {
                if (animationState.boxPathActive || animationState.boxPathNeedsFinalClear) {
                    renderBoxPathOrFull()
                } else {
                    renderVanishDirty()
                }
            }
            if (!changed && !vanishChanged && !blinkActive && playerFlashActive && !animationState.boxPathActive) {
                renderPlayerFlashDirty()
            }
            if (!playerFlashActive && animationState.playerFlashPosition != null && !animationState.boxPathActive) {
                val clearedPos = animationState.playerFlashPosition
                animationState.playerFlashPosition = null
                animationState.playerFlashStartMs = 0L
                if (clearedPos != null) {
                    val viewport = checkNotNull(lastViewport) { "Dirty render requested without viewport." }
                    val rect = spriteDrawParams(viewport, clearedPos, 0.80f).dirtyRect
                    renderDirty(rect)
                }
            }
            if (transitionActive && !changed && !vanishChanged && !blinkActive && !animationState.boxPathActive) {
                render()
            }
            if (transitionState.transition?.isComplete(now) == true) {
                transitionState.transition = null
                render()
            }
            if (animationState.vanishNeedsFinalClear && animationState.vanishPosition == null) {
                renderVanishDirty()
                animationState.vanishNeedsFinalClear = false
                animationState.vanishLastPosition = null
            }
            // If we just completed a box-path animation, drop the cached dirty rect after the first
            // post-animation draw clears the line.
            if (animationState.boxPathNeedsFinalClear && !animationState.boxPathActive) {
                animationState.boxPathDirtyRect = null
                animationState.boxPathNeedsFinalClear = false
            }
            val vanishActive = animationState.vanishPosition != null
            if (animationState.boxPathActive || blinkActive || vanishActive || transitionActive) {
                postOnAnimation(this)
            } else if (pendingBlink) {
                val delay = (animationState.blinkStartMs - now).coerceAtLeast(0L)
                postDelayed(this, delay)
            } else if (playerFlashActive) {
                postOnAnimation(this)
            }
        }
    }
    init {
        holder.addCallback(this)
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val viewport = lastViewport ?: return@setOnTouchListener true
                val position = viewport.screenToInnerCell(event.x, event.y)
                if (position != null) {
                    onTapCell?.invoke(position)
                }
                return@setOnTouchListener true
            }
            true
        }
    }

    fun setOnTapCell(onTapCell: (Position) -> Unit) {
        this.onTapCell = onTapCell
    }

    fun applyDelta(delta: GameController.RenderDelta) {
        when (delta) {
            is GameController.RenderDelta.LevelLoaded -> {
                loadLevel(
                    LevelInit(
                        tiles = delta.tiles,
                        playerPosition = delta.playerPosition,
                        boxPositions = delta.boxPositions
                    )
                )
            }
            is GameController.RenderDelta.PlayerMoved -> onPlayerMoved(to = delta.to)
            is GameController.RenderDelta.BoxMoved -> onBoxMoved(path = delta.path)
            is GameController.RenderDelta.MoveRejected -> onMoveRejected()
            is GameController.RenderDelta.GameWon -> onGameWon(isClean = delta.isClean)
        }
    }

    fun loadLevel(init: LevelInit) {
        val nowMs = SystemClock.elapsedRealtime()
        val newTiles = init.tiles.map { it.toList() }
        val isSameLayout = renderState.isInitialized && renderState.tiles == newTiles
        val shouldAnimate = init.tiles.isNotEmpty() && !isSameLayout
        if (shouldAnimate) {
            transitionState.transition = LevelTransition(
                oldTiles = renderState.tiles,
                newTiles = newTiles,
                startMs = nowMs
            )
            removeCallbacks(animationFrameRunnable)
            postOnAnimation(animationFrameRunnable)
        } else {
            transitionState.transition = null
        }
        renderState.tiles = newTiles
        renderState.boxPositions = init.boxPositions.toSet()
        renderState.playerPosition = init.playerPosition
        renderState.displayedPlayerPosition = init.playerPosition
        renderState.pendingPlayerPosition = null
        renderState.selectedBox = null
        resetFacing()
        animationState.boxPath = emptyList()
        animationState.boxPathActive = false
        animationState.boxPathShrink = 0f
        animationState.boxPathDirtyRect = null
        animationState.boxPathNeedsFinalClear = false
        animationState.boxPathSuppressLine = false
        animationState.boxFlashPosition = null
        animationState.boxFlashStartMs = 0L
        animationState.playerSilhouettePosition = null
        animationState.playerSilhouetteStartMs = 0L
        animationState.playerFlashPosition = null
        animationState.playerFlashStartMs = 0L
        animationState.blinkStartMs = 0L
        animationState.blinkEndMs = 0L
        animationState.lastBlinkActive = false
        animationState.vanishPosition = null
        animationState.vanishStep = null
        animationState.vanishStartMs = 0L
        animationState.vanishLastPosition = null
        animationState.vanishNeedsFinalClear = false
        renderState.isInitialized = true
        rebuildStaticFrameIfPossible()
        render()
    }

    fun setSelectedBox(selected: Position?) {
        if (!renderState.isInitialized) return
        val previous = renderState.selectedBox
        if (previous == selected) return

        renderState.selectedBox = selected

        val viewport = lastViewport
        checkNotNull(viewport) { "Dirty render requested without viewport." }
        var dirty: Rect? = null
        if (previous != null) {
            dirty = Rect(spriteDrawParams(viewport, previous, 0.90f).dirtyRect)
        }
        if (selected != null) {
            val rect = spriteDrawParams(viewport, selected, 0.90f).dirtyRect
            if (dirty == null) {
                dirty = Rect(rect)
            } else {
                dirty.union(rect)
            }
        }
        if (dirty == null) return
        renderDirty(dirty)
    }

    fun getSelectedBox(): Position? = renderState.selectedBox

    fun onPlayerMoved(to: Position) {
        if (!renderState.isInitialized) return

        val from = renderState.playerPosition
        resetFacing()
        renderState.playerPosition = to
        renderState.displayedPlayerPosition = to
        animationState.playerFlashPosition = from
        animationState.playerFlashStartMs = SystemClock.elapsedRealtime()
        removeCallbacks(animationFrameRunnable)
        postOnAnimation(animationFrameRunnable)

        checkNotNull(from) { "Dirty render requested without previous player position." }
        val viewport = checkNotNull(lastViewport) { "Dirty render requested without viewport." }
        val fromParams = spriteDrawParams(viewport, from, 0.80f)
        val toParams = spriteDrawParams(viewport, to, 0.80f)
        val dirty = Rect(fromParams.dirtyRect)
        dirty.union(toParams.dirtyRect)
        renderDirty(dirty)
    }

    fun onMoveRejected() {
        if (!renderState.isInitialized) return
        triggerBlink()
    }

    fun onGameWon(isClean: Boolean) {
        if (!renderState.isInitialized) return
        if (!isClean) {
            triggerBlink()
        }
    }

    fun onBoxMoved(path: List<Position>) {
        if (!renderState.isInitialized) return
        if (path.size < 2) return
        val from = path.first()
        val to = path.last()
        if (renderState.tiles[to.row][to.col] == Tile.WALL) {
            renderState.boxPositions = renderState.boxPositions - from
            startVanishBoxAnimation(at = to)
        } else {
            renderState.boxPositions = (renderState.boxPositions - from) + to
        }
        for (i in path.size - 1 downTo 1) {
            val prev = path[i - 1]
            val curr = path[i]
            if (curr.col != prev.col) {
                renderState.pendingFacingLeft = curr.col < prev.col
                break
            }
        }
        renderState.displayedPlayerPosition = renderState.playerPosition
        renderState.playerPosition = path[path.size - 2]
        animationState.playerSilhouettePosition = renderState.displayedPlayerPosition
        animationState.playerSilhouetteStartMs = SystemClock.elapsedRealtime()
        animationState.boxFlashPosition = from
        animationState.boxFlashStartMs = animationState.playerSilhouetteStartMs
        animationState.boxPathSuppressLine = path.size == 2
        startBoxPathAnimation(path, renderState.playerPosition ?: path[path.size - 2])
        renderBoxPathOrFull()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (renderState.isInitialized) {
            rebuildStaticFrameIfPossible()
            render()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (renderState.isInitialized) {
            rebuildStaticFrameIfPossible()
            render()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        removeCallbacks(animationFrameRunnable)
        staticFrameBitmap?.recycle()
        staticFrameBitmap = null
        backgroundBitmap?.recycle()
        backgroundBitmap = null
    }

    private fun render() {
        if (width <= 0 || height <= 0) return
        if (!renderState.isInitialized) return
        val playerPosition = if (animationState.boxPathActive) {
            renderState.displayedPlayerPosition ?: renderState.playerPosition!!
        } else {
            renderState.playerPosition!!
        }
        val innerRows = renderState.tiles.size
        val innerCols = renderState.tiles.first().size
        val viewport = computeBoardViewport(width.toFloat(), height.toFloat(), innerRows, innerCols)
        lastViewport = viewport

        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return
        try {
            drawScene(
                canvas = canvas,
                viewport = viewport,
                tiles = renderState.tiles,
                boxPositions = renderState.boxPositions,
                playerPosition = playerPosition,
                selectedBox = renderState.selectedBox,
                isFacingLeft = renderState.isFacingLeft,
                drawPlayer = true
            )
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun rebuildStaticFrameIfPossible() {
        if (width <= 0 || height <= 0) return
        if (!renderState.isInitialized) return
        if (renderState.tiles.isEmpty()) return
        val firstRow = renderState.tiles.firstOrNull() ?: return
        if (firstRow.isEmpty()) return

        val existing = staticFrameBitmap
        if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
            return
        }

        existing?.recycle()

        val bitmap = createBitmap(width, height)
        val bitmapCanvas = Canvas(bitmap)

        val innerRows = renderState.tiles.size
        val innerCols = renderState.tiles.first().size
        val viewport = computeBoardViewport(width.toFloat(), height.toFloat(), innerRows, innerCols)
        // Keep this consistent with full render so touch mapping stays correct.
        lastViewport = viewport

        drawBackground(bitmapCanvas)

        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        val halfStroke = floorStrokePaint.strokeWidth / 2f

        for ((rowIndex, row) in renderState.tiles.withIndex()) {
            for ((colIndex, tile) in row.withIndex()) {
                val tileLeft = offsetX + (colIndex + 1) * cellSize
                val tileTop = offsetY + (rowIndex + 1) * cellSize
                val tileRight = tileLeft + cellSize
                val tileBottom = tileTop + cellSize
                drawTileCell(bitmapCanvas, tile, tileLeft, tileTop, tileRight, tileBottom, halfStroke)
            }
        }

        staticFrameBitmap = bitmap
    }

    private fun renderDirty(requestedDirtyRect: Rect) {
        if (width <= 0 || height <= 0) return
        if (!renderState.isInitialized) return

        check(!animationState.boxPathActive) { "Dirty render requested during box path animation." }

        val viewport = lastViewport
        checkNotNull(viewport) { "Dirty render requested without viewport." }

        val dirtyRect = Rect(requestedDirtyRect)
        if (!dirtyRect.intersect(0, 0, width, height)) return
        if (!holder.surface.isValid) return

        val playerPos = renderState.playerPosition ?: return

        val canvas = holder.lockCanvas(dirtyRect) ?: return
        try {
            canvas.save()
            canvas.clipRect(dirtyRect)

            drawScene(
                canvas = canvas,
                viewport = viewport,
                tiles = renderState.tiles,
                boxPositions = renderState.boxPositions,
                playerPosition = playerPos,
                selectedBox = renderState.selectedBox,
                isFacingLeft = renderState.isFacingLeft,
                drawPlayer = true
            )
            if (!animationState.boxPathActive && animationState.playerFlashPosition != null) {
                val nowMs = SystemClock.elapsedRealtime()
                val elapsedMs = nowMs - animationState.playerFlashStartMs
                if (elapsedMs <= RenderTimings.FLASH_DURATION_MS) {
                    val flashPos = animationState.playerFlashPosition
                    if (flashPos != null) {
                        val params = spriteDrawParams(viewport, flashPos, 0.80f)
                        val body = assets.getBitmap(R.drawable.player_slime, params.sizePx)
                        drawFlashedSprite(
                            canvas = canvas,
                            bitmap = body,
                            left = params.left,
                            top = params.top,
                            sizePx = params.sizePx,
                            flipX = renderState.isFacingLeft,
                            elapsedMs = elapsedMs,
                            darkPaint = playerFlashDarkPaint,
                            lightPaint = playerFlashLightPaint
                        )
                    }
                } else {
                    animationState.playerFlashPosition = null
                    animationState.playerFlashStartMs = 0L
                }
            }
        } finally {
            canvas.restore()
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun renderBoxPathOrFull() {
        val viewport = lastViewport
        checkNotNull(viewport) { "Dirty render requested without viewport." }
        val boxDirty = animationState.boxPathDirtyRect
        val vanishTarget = if (animationState.vanishPosition != null || animationState.vanishNeedsFinalClear) {
            animationState.vanishPosition ?: animationState.vanishLastPosition
        } else {
            null
        }
        val vanishDirty = vanishTarget?.let { computeVanishDirtyRect(viewport, it) }
        var dirty = when {
            boxDirty != null && vanishDirty != null -> Rect(boxDirty).apply { union(vanishDirty) }
            boxDirty != null -> Rect(boxDirty)
            vanishDirty != null -> Rect(vanishDirty)
            else -> null
        }
        val nowMs = SystemClock.elapsedRealtime()
        if (animationState.boxFlashPosition != null &&
            nowMs - animationState.boxFlashStartMs <= RenderTimings.FLASH_DURATION_MS) {
            val flashRect = spriteDrawParams(viewport, animationState.boxFlashPosition!!, 0.90f).dirtyRect
            dirty = dirty?.apply { union(flashRect) } ?: Rect(flashRect)
        }
        val silhouettePos = animationState.playerSilhouettePosition
        if (silhouettePos != null) {
            val silhouetteRect = spriteDrawParams(viewport, silhouettePos, 0.80f).dirtyRect
            dirty = dirty?.apply { union(silhouetteRect) } ?: Rect(silhouetteRect)
        }
        val playerFlashPos = animationState.playerFlashPosition
        if (playerFlashPos != null &&
            nowMs - animationState.playerFlashStartMs <= RenderTimings.FLASH_DURATION_MS) {
            val flashRect = spriteDrawParams(viewport, playerFlashPos, 0.80f).dirtyRect
            dirty = dirty?.apply { union(flashRect) } ?: Rect(flashRect)
        }
        if (dirty == null) {
            error("Dirty render requested without dirty rect.")
        }
        renderDirtyForBoxPath(dirty)
    }

    private fun renderDirtyForBoxPath(requestedDirtyRect: Rect) {
        if (width <= 0 || height <= 0) return
        if (!renderState.isInitialized) return

        val viewport = lastViewport
        checkNotNull(viewport) { "Dirty render requested without viewport." }

        val dirtyRect = Rect(requestedDirtyRect)
        if (!dirtyRect.intersect(0, 0, width, height)) return
        if (!holder.surface.isValid) return

        val effectivePlayer = if (animationState.boxPathActive) {
            renderState.displayedPlayerPosition ?: renderState.playerPosition
        } else {
            renderState.playerPosition
        } ?: return

        val canvas = holder.lockCanvas(dirtyRect) ?: return
        try {
            canvas.save()
            canvas.clipRect(dirtyRect)

            drawScene(
                canvas = canvas,
                viewport = viewport,
                tiles = renderState.tiles,
                boxPositions = renderState.boxPositions,
                playerPosition = effectivePlayer,
                selectedBox = renderState.selectedBox,
                isFacingLeft = renderState.isFacingLeft,
                drawPlayer = !animationState.boxPathActive
            )
            if (animationState.boxPathActive && animationState.boxFlashPosition != null) {
                val nowMs = SystemClock.elapsedRealtime()
                val elapsedMs = nowMs - animationState.boxFlashStartMs
                if (elapsedMs <= RenderTimings.FLASH_DURATION_MS) {
                    val flashPos = animationState.boxFlashPosition
                    if (flashPos != null) {
                        val params = spriteDrawParams(viewport, flashPos, 0.90f)
                        val bitmap = assets.getBitmap(R.drawable.box, params.sizePx)
                        drawFlashedBitmap(
                            canvas = canvas,
                            bitmap = bitmap,
                            left = params.left,
                            top = params.top,
                            elapsedMs = elapsedMs,
                            darkPaint = boxFlashDarkPaint,
                            lightPaint = boxFlashLightPaint
                        )
                    }
                } else {
                    animationState.boxFlashPosition = null
                    animationState.boxFlashStartMs = 0L
                }
            }
            if (animationState.boxPathActive && animationState.playerSilhouettePosition != null) {
                val nowMs = SystemClock.elapsedRealtime()
                val elapsedMs = nowMs - animationState.playerSilhouetteStartMs
                if (elapsedMs <= RenderTimings.FLASH_DURATION_MS) {
                    val silhouettePosition = animationState.playerSilhouettePosition
                    if (silhouettePosition != null) {
                        val params = spriteDrawParams(viewport, silhouettePosition, 0.80f)
                        val body = assets.getBitmap(R.drawable.player_slime, params.sizePx)
                        drawFlashedSprite(
                            canvas = canvas,
                            bitmap = body,
                            left = params.left,
                            top = params.top,
                            sizePx = params.sizePx,
                            flipX = renderState.isFacingLeft,
                            elapsedMs = elapsedMs,
                            darkPaint = playerSilhouetteDarkPaint,
                            lightPaint = playerSilhouetteLightPaint
                        )
                    }
                } else {
                    animationState.playerSilhouettePosition = null
                    animationState.playerSilhouetteStartMs = 0L
                }
            }
            if (!animationState.boxPathActive && animationState.playerFlashPosition != null) {
                val nowMs = SystemClock.elapsedRealtime()
                val elapsedMs = nowMs - animationState.playerFlashStartMs
                if (elapsedMs <= RenderTimings.FLASH_DURATION_MS) {
                    val flashPos = animationState.playerFlashPosition
                    if (flashPos != null) {
                        val params = spriteDrawParams(viewport, flashPos, 0.80f)
                        val body = assets.getBitmap(R.drawable.player_slime, params.sizePx)
                        drawFlashedSprite(
                            canvas = canvas,
                            bitmap = body,
                            left = params.left,
                            top = params.top,
                            sizePx = params.sizePx,
                            flipX = renderState.isFacingLeft,
                            elapsedMs = elapsedMs,
                            darkPaint = playerFlashDarkPaint,
                            lightPaint = playerFlashLightPaint
                        )
                    }
                } else {
                    animationState.playerFlashPosition = null
                    animationState.playerFlashStartMs = 0L
                }
            }
        } finally {
            canvas.restore()
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun renderVanishDirty() {
        if (!renderState.isInitialized) return
        val viewport = checkNotNull(lastViewport) { "Dirty render requested without viewport." }
        val position = animationState.vanishPosition ?: animationState.vanishLastPosition ?: return
        renderDirty(computeVanishDirtyRect(viewport, position))
    }

    private fun renderPlayerFlashDirty() {
        if (!renderState.isInitialized) return
        if (animationState.boxPathActive) {
            renderBoxPathOrFull()
            return
        }
        val viewport = checkNotNull(lastViewport) { "Dirty render requested without viewport." }
        val position = animationState.playerFlashPosition ?: return
        val rect = spriteDrawParams(viewport, position, 0.80f).dirtyRect
        renderDirty(rect)
    }

    private fun renderBlinkDirty() {
        if (!renderState.isInitialized) return
        if (animationState.boxPathActive || animationState.vanishPosition != null) {
            renderBoxPathOrFull()
            return
        }
        val viewport = checkNotNull(lastViewport) { "Dirty render requested without viewport." }
        val playerPos = renderState.playerPosition ?: return
        val params = spriteDrawParams(viewport, playerPos, 0.80f)
        val openBounds = assets.getOpaqueBounds(R.drawable.player_eyes_open, params.sizePx)
        val blinkBounds = assets.getOpaqueBounds(R.drawable.player_eyes_blink, params.sizePx)
        val bounds = Rect(openBounds)
        bounds.union(blinkBounds)
        if (bounds.isEmpty) {
            error("Dirty render requested with empty blink bounds.")
        }
        val paddingPx = 2
        val left = params.left.toInt() + bounds.left - paddingPx
        val top = params.top.toInt() + bounds.top - paddingPx
        val right = params.left.toInt() + bounds.right + paddingPx
        val bottom = params.top.toInt() + bounds.bottom + paddingPx
        renderDirty(Rect(left, top, right, bottom))
    }

    private fun isBlinking(nowMs: Long): Boolean {
        return nowMs in animationState.blinkStartMs until animationState.blinkEndMs
    }

    private fun triggerBlink(delayMs: Long = RenderTimings.BLINK_DELAY_MS) {
        val nowMs = SystemClock.elapsedRealtime()
        val start = nowMs + delayMs
        animationState.blinkStartMs = start
        animationState.blinkEndMs = start + RenderTimings.BLINK_DURATION_MS
        animationState.lastBlinkActive = false
        val delay = (animationState.blinkStartMs - nowMs).coerceAtLeast(0L)
        removeCallbacks(animationFrameRunnable)
        removeCallbacks(blinkStartRunnable)
        if (delay == 0L) {
            renderBlinkDirty()
            postOnAnimation(animationFrameRunnable)
        } else {
            postDelayed(blinkStartRunnable, delay)
        }
    }

    private fun resetFacing() {
        renderState.isFacingLeft = false
        renderState.pendingFacingLeft = null
    }
    private fun requireBackgroundBitmap(): Bitmap {
        val existing = backgroundBitmap
        if (existing != null && !existing.isRecycled) return existing

        val decoded = BitmapFactory.decodeResource(resources, R.drawable.bg_space)
        require(!decoded.isRecycled)
        backgroundBitmap = decoded
        return decoded
    }

    private fun drawTileCell(
        canvas: Canvas,
        tile: Tile,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        halfStroke: Float
    ) {
        when (tile) {
            Tile.WALL -> Unit
            Tile.FLOOR -> {
                canvas.drawRect(left, top, right, bottom, floorFillPaint)
                canvas.drawRect(
                    left + halfStroke,
                    top + halfStroke,
                    right - halfStroke,
                    bottom - halfStroke,
                    floorStrokePaint
                )
            }
            Tile.GOAL -> {
                canvas.drawRect(left, top, right, bottom, goalFillPaint)
                canvas.drawRect(
                    left + halfStroke,
                    top + halfStroke,
                    right - halfStroke,
                    bottom - halfStroke,
                    goalStrokePaint
                )
            }
        }
    }


    private fun drawBackground(canvas: Canvas) {
        val viewW = width
        val viewH = height
        require(viewW > 0 && viewH > 0)

        val bitmap = requireBackgroundBitmap()
        val bmpW = bitmap.width
        val bmpH = bitmap.height
        require(bmpW > 0 && bmpH > 0)

        val viewAspect = viewW.toFloat() / viewH.toFloat()
        val bmpAspect = bmpW.toFloat() / bmpH.toFloat()

        if (bmpAspect > viewAspect) {
            // Bitmap is wider than the view; crop left/right.
            val srcW = (bmpH * viewAspect).roundToInt().coerceAtMost(bmpW)
            val left = ((bmpW - srcW) / 2f).roundToInt().coerceAtLeast(0)
            backgroundSrcRect.set(left, 0, left + srcW, bmpH)
        } else {
            // Bitmap is taller than the view; crop top/bottom.
            val srcH = (bmpW / viewAspect).roundToInt().coerceAtMost(bmpH)
            val top = ((bmpH - srcH) / 2f).roundToInt().coerceAtLeast(0)
            backgroundSrcRect.set(0, top, bmpW, top + srcH)
        }

        backgroundDstRect.set(0, 0, viewW, viewH)
        canvas.drawBitmap(bitmap, backgroundSrcRect, backgroundDstRect, backgroundPaint)
    }

    private fun drawScene(
        canvas: Canvas,
        viewport: BoardViewport,
        tiles: List<List<Tile>>,
        boxPositions: Set<Position>,
        playerPosition: Position,
        selectedBox: Position?,
        isFacingLeft: Boolean,
        drawPlayer: Boolean
    ) {
        drawBackground(canvas)
        val transition = transitionState.transition
        if (transition != null) {
            val nowMs = SystemClock.elapsedRealtime()
            if (transition.isComplete(nowMs)) {
                transitionState.transition = null
            } else {
                drawTransitionTiles(canvas, viewport, transition)
                drawTransitionEntities(
                    canvas = canvas,
                    viewport = viewport,
                    transition = transition,
                    nowMs = nowMs,
                    drawPlayer = drawPlayer
                )
                return
            }
        }
        val bitmapPaint = assets.bitmapPaint()
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        val halfStroke = floorStrokePaint.strokeWidth / 2f

        for ((rowIndex, row) in tiles.withIndex()) {
            for ((colIndex, tile) in row.withIndex()) {
                val tileLeft = offsetX + (colIndex + 1) * cellSize
                val tileTop = offsetY + (rowIndex + 1) * cellSize
                val tileRight = tileLeft + cellSize
                val tileBottom = tileTop + cellSize
                drawTileCell(canvas, tile, tileLeft, tileTop, tileRight, tileBottom, halfStroke)
            }
        }

        if (animationState.boxPathActive && !animationState.boxPathSuppressLine) {
            drawBoxPathLine(
                canvas = canvas,
                viewport = viewport,
                path = animationState.boxPath,
                shrink = animationState.boxPathShrink
            )
        }

        val vanishPos = animationState.vanishPosition
        if (vanishPos != null) {
            drawVanishingBox(
                canvas = canvas,
                viewport = viewport,
                gridPosition = vanishPos,
                paddedPosition = Position(vanishPos.row + 1, vanishPos.col + 1)
            )
        }

        for (position in boxPositions) {
            val origin = Position(position.row + 1, position.col + 1)
                .toRenderPoint(cellSize, offsetX, offsetY)
            val targetSize = snapToWholePixel(cellSize * 0.90f)
            val sizePx = targetSize.toInt()
            require(sizePx > 0)
            val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
            val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
            val resId =
                if (selectedBox == position) R.drawable.box_selected else R.drawable.box
            val bitmap = assets.getBitmap(resId, sizePx)
            canvas.drawBitmap(bitmap, left, top, bitmapPaint)
        }

        if (drawPlayer) {
            val origin = Position(playerPosition.row + 1, playerPosition.col + 1)
                .toRenderPoint(cellSize, offsetX, offsetY)
            val targetSize = snapToWholePixel(cellSize * 0.80f)
            val sizePx = targetSize.toInt()
            require(sizePx > 0)
            val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
            val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
            val body = assets.getBitmap(R.drawable.player_slime, sizePx)
            val eyesRes = if (isBlinking(SystemClock.elapsedRealtime())) {
                R.drawable.player_eyes_blink
            } else {
                R.drawable.player_eyes_open
            }
            val eyes = assets.getBitmap(eyesRes, sizePx)
            drawSprite(canvas, body, left, top, sizePx, isFacingLeft, bitmapPaint)
            drawSprite(canvas, eyes, left, top, sizePx, isFacingLeft, bitmapPaint)
        }

    }

    private fun drawTransitionTiles(
        canvas: Canvas,
        viewport: BoardViewport,
        transition: LevelTransition
    ) {
        val nowMs = SystemClock.elapsedRealtime()
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        val halfStroke = floorStrokePaint.strokeWidth / 2f
        val rows = transition.rows
        val cols = transition.cols
        for (rowIndex in 0 until rows) {
            for (colIndex in 0 until cols) {
                val newTile = transition.tileAt(transition.newTiles, rowIndex, colIndex)
                val growScale = if (newTile != Tile.WALL) transition.growScale(nowMs, rowIndex, colIndex) else null
                if (growScale != null) {
                    drawScaledTile(
                        canvas = canvas,
                        tile = newTile,
                        rowIndex = rowIndex,
                        colIndex = colIndex,
                        scale = growScale,
                        cellSize = cellSize,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        halfStroke = halfStroke
                    )
                }
            }
        }
    }

    private fun drawTransitionEntities(
        canvas: Canvas,
        viewport: BoardViewport,
        transition: LevelTransition,
        nowMs: Long,
        drawPlayer: Boolean
    ) {
        val bitmapPaint = assets.bitmapPaint()
        for (position in renderState.boxPositions) {
            if (!transition.isCellReady(nowMs, position.row, position.col)) continue
            val params = spriteDrawParams(viewport, position, 0.90f)
            val resId = if (renderState.selectedBox == position) R.drawable.box_selected else R.drawable.box
            val bitmap = assets.getBitmap(resId, params.sizePx)
            canvas.drawBitmap(bitmap, params.left, params.top, bitmapPaint)
        }

        if (drawPlayer) {
            val playerPos = renderState.playerPosition
            if (playerPos != null && transition.isCellReady(nowMs, playerPos.row, playerPos.col)) {
                val params = spriteDrawParams(viewport, playerPos, 0.80f)
                val body = assets.getBitmap(R.drawable.player_slime, params.sizePx)
                val eyesRes = if (isBlinking(SystemClock.elapsedRealtime())) {
                    R.drawable.player_eyes_blink
                } else {
                    R.drawable.player_eyes_open
                }
                val eyes = assets.getBitmap(eyesRes, params.sizePx)
                drawSprite(canvas, body, params.left, params.top, params.sizePx, renderState.isFacingLeft, bitmapPaint)
                drawSprite(canvas, eyes, params.left, params.top, params.sizePx, renderState.isFacingLeft, bitmapPaint)
            }
        }
    }

    private fun drawScaledTile(
        canvas: Canvas,
        tile: Tile,
        rowIndex: Int,
        colIndex: Int,
        scale: Float,
        cellSize: Float,
        offsetX: Float,
        offsetY: Float,
        halfStroke: Float
    ) {
        if (tile == Tile.WALL) return
        val tileLeft = offsetX + (colIndex + 1) * cellSize
        val tileTop = offsetY + (rowIndex + 1) * cellSize
        val centerX = tileLeft + cellSize / 2f
        val centerY = tileTop + cellSize / 2f
        val size = cellSize * scale
        val left = centerX - size / 2f
        val top = centerY - size / 2f
        val right = centerX + size / 2f
        val bottom = centerY + size / 2f
        when (tile) {
            Tile.FLOOR -> {
                canvas.drawRect(left, top, right, bottom, floorFillPaint)
                canvas.drawRect(
                    left + halfStroke,
                    top + halfStroke,
                    right - halfStroke,
                    bottom - halfStroke,
                    floorStrokePaint
                )
            }
            Tile.GOAL -> {
                canvas.drawRect(left, top, right, bottom, goalFillPaint)
                canvas.drawRect(
                    left + halfStroke,
                    top + halfStroke,
                    right - halfStroke,
                    bottom - halfStroke,
                    goalStrokePaint
                )
            }
            else -> Unit
        }
    }

    private fun startVanishBoxAnimation(at: Position) {
        animationState.vanishPosition = at
        animationState.vanishStartMs = SystemClock.elapsedRealtime()
        animationState.vanishStep = 0
        animationState.vanishLastPosition = at
        animationState.vanishNeedsFinalClear = false
        removeCallbacks(animationFrameRunnable)
        postOnAnimation(animationFrameRunnable)
    }

    private fun startBoxPathAnimation(path: List<Position>, pendingPlayer: Position) {
        require(path.size >= 2) { "Box path requires at least two points." }
        animationState.boxPath = path
        renderState.pendingPlayerPosition = pendingPlayer
        animationState.boxPathStartMs = SystemClock.elapsedRealtime()
        animationState.boxPathShrink = 0f
        animationState.boxPathActive = true
        animationState.boxPathNeedsFinalClear = false

        // Conservative dirty rect: entire path line + moved box + both displayed and pending player sprites.
        val viewport = lastViewport
            ?: run {
                if (width > 0 && height > 0 &&
                    renderState.tiles.isNotEmpty() &&
                    renderState.tiles.first().isNotEmpty()) {
                    computeBoardViewport(
                        width.toFloat(),
                        height.toFloat(),
                        renderState.tiles.size,
                        renderState.tiles.first().size
                    )
                } else {
                    null
                }
            }

        if (viewport != null) {
            // Ensure future partial renders have a viewport.
            lastViewport = viewport
            val displayedPlayer = renderState.displayedPlayerPosition ?: pendingPlayer
            animationState.boxPathDirtyRect =
                computeBoxPathDirtyRect(viewport, path, displayedPlayer, pendingPlayer)
        } else {
            animationState.boxPathDirtyRect = null
        }

        removeCallbacks(animationFrameRunnable)
        postOnAnimation(animationFrameRunnable)
    }

    private fun updateBoxPathAnimation(nowMs: Long): Boolean {
        if (!animationState.boxPathActive) return false
        val elapsed = nowMs - animationState.boxPathStartMs
        if (elapsed < RenderTimings.BOX_PATH_DELAY_MS) return false
        val progress = if (animationState.boxPathSuppressLine) {
            1f
        } else {
            ((elapsed - RenderTimings.BOX_PATH_DELAY_MS).toFloat() /
                RenderTimings.BOX_PATH_DURATION_MS.toFloat()).coerceAtMost(1f)
        }
        var changed = false
        if (progress != animationState.boxPathShrink) {
            animationState.boxPathShrink = progress
            changed = true
        }
        if (elapsed >= RenderTimings.BOX_PATH_DELAY_MS +
            if (animationState.boxPathSuppressLine) 0L else RenderTimings.BOX_PATH_DURATION_MS) {
            animationState.boxPathActive = false
            animationState.boxPathNeedsFinalClear = true
            animationState.boxPathSuppressLine = false
            val pending = renderState.pendingPlayerPosition
            if (pending != null) {
                renderState.displayedPlayerPosition = pending
                renderState.pendingPlayerPosition = null
            }
            renderState.pendingFacingLeft?.let { facing ->
                renderState.isFacingLeft = facing
            }
            renderState.pendingFacingLeft = null
            changed = true
        }
        return changed
    }

    private fun updateVanishAnimation(nowMs: Long): Boolean {
        val currentPosition = animationState.vanishPosition ?: return false
        val elapsed = nowMs - animationState.vanishStartMs
        var cumulative = 0L

        for (step in 0..VanishSpec.LAST_STEP) {
            val delay = VanishSpec.delayMs(step)
            if (elapsed < cumulative + delay) {
                if (animationState.vanishStep != step) {
                    animationState.vanishStep = step
                    return true
                }
                return false
            }
            cumulative += delay
        }

        if (animationState.vanishStep != null) {
            animationState.vanishStep = null
            animationState.vanishPosition = null
            animationState.vanishLastPosition = currentPosition
            animationState.vanishNeedsFinalClear = true
            triggerBlink(delayMs = 0L)
            return true
        }

        return false
    }

    private fun drawBoxPathLine(
        canvas: Canvas,
        viewport: BoardViewport,
        path: List<Position>,
        shrink: Float
    ) {
        if (path.size < 2) return
        if (animationState.boxPathSuppressLine) return
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - animationState.boxPathStartMs < RenderTimings.BOX_PATH_DELAY_MS) return
        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY
        val strokeWidth = cellSize * 0.2f
        boxPathPaint.strokeWidth = strokeWidth
        boxPathTailPaint.strokeWidth = strokeWidth

        val points = path.map { position ->
            val cx = offsetX + (position.col + 1) * cellSize + cellSize / 2f
            val cy = offsetY + (position.row + 1) * cellSize + cellSize / 2f
            android.graphics.PointF(cx, cy)
        }

        val totalSegments = points.size - 1
        val startT = totalSegments.toFloat() * shrink.coerceIn(0f, 1f)
        val startSegment = startT.toInt().coerceIn(0, totalSegments - 1)
        val startFraction = startT - startSegment

        fun interpolate(start: android.graphics.PointF, end: android.graphics.PointF, t: Float): android.graphics.PointF {
            return android.graphics.PointF(
                start.x + (end.x - start.x) * t,
                start.y + (end.y - start.y) * t
            )
        }

        val startPoint = interpolate(points[startSegment], points[startSegment + 1], startFraction)

        val tailParams = spriteDrawParams(viewport, path[startSegment], 0.90f)
        val tailOpaque = assets.getOpaqueBounds(R.drawable.box, tailParams.sizePx)
        if (!tailOpaque.isEmpty) {
            val left = tailParams.left + tailOpaque.left
            val top = tailParams.top + tailOpaque.top
            val right = tailParams.left + tailOpaque.right
            val bottom = tailParams.top + tailOpaque.bottom
            canvas.drawRect(left, top, right, bottom, boxPathTailPaint)
        }

        var prev = startPoint
        var drewAnySegment = false
        for (index in (startSegment + 1) until points.size) {
            val next = points[index]
            canvas.drawLine(prev.x, prev.y, next.x, next.y, boxPathPaint)
            prev = next
            drewAnySegment = true
        }

        if (!drewAnySegment) {
            canvas.drawCircle(startPoint.x, startPoint.y, strokeWidth / 2f, boxPathDotPaint)
        }
    }


    private fun drawVanishingBox(
        canvas: Canvas,
        viewport: BoardViewport,
        gridPosition: Position,
        paddedPosition: Position
    ) {
        val currentPosition = animationState.vanishPosition ?: return
        val step = animationState.vanishStep ?: return
        if (currentPosition != gridPosition) return
        require(step in 0..VanishSpec.LAST_STEP) { "Vanish step out of range: $step" }

        val cellSize = viewport.cellSize
        val offsetX = viewport.offsetX
        val offsetY = viewport.offsetY

        val origin = paddedPosition.toRenderPoint(cellSize, offsetX, offsetY)
        val targetSize = snapToWholePixel(cellSize * 0.90f)
        val sizePx = targetSize.toInt()
        require(sizePx > 0)
        val leftPx = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
        val topPx = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
        val scale = VanishSpec.scale(step)
        val size = targetSize * scale
        if (size <= 0f) return
        val left = leftPx + (targetSize - size) / 2f
        val top = topPx + (targetSize - size) / 2f
        val bitmap = assets.getBitmap(R.drawable.box, sizePx)
        canvas.withClip(left, top, left + size, top + size) {
            drawBitmap(bitmap, leftPx, topPx, assets.bitmapPaint())
        }
    }
}
