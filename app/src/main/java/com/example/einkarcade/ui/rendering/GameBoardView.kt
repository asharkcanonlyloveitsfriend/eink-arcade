package com.example.einkarcade.ui.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.TileMap
import com.example.einkarcade.ui.GameRenderEvent
import com.example.einkarcade.ui.rendering.anim.AnimationRunner
import com.example.einkarcade.ui.rendering.anim.BlinkAnimation
import com.example.einkarcade.ui.rendering.anim.BoxPathAnimation
import com.example.einkarcade.ui.rendering.anim.BoxVanishAnimation
import com.example.einkarcade.ui.rendering.anim.EntityFlashAnimation
import com.example.einkarcade.ui.rendering.draw.EntityDrawer
import com.example.einkarcade.ui.rendering.draw.EntityRenderer
import com.example.einkarcade.ui.rendering.draw.StaticBoardRenderer
import com.example.einkarcade.ui.rendering.draw.TileDrawer
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import com.example.einkarcade.ui.rendering.geom.screenToInnerCell

@SuppressLint("ClickableViewAccessibility")
internal class GameBoardView(
    context: Context,
) : View(context) {
    private val assets = AndroidGameAssets(context)
    private val entityRenderer =
        EntityRenderer(
            assets = assets,
            entityDrawer = EntityDrawer(assets),
        )
    private val staticBoardRenderer =
        StaticBoardRenderer(
            context = context,
            tileDrawer = TileDrawer(),
        )

    private var staticFrame: StaticBoardFrame? = null
    private var boxPositions: Set<Position> = emptySet()
    private var playerPosition: Position? = null

    private var onTapCell: ((Position) -> Unit)? = null
    var selectedBox: Position? = null
        set(value) {
            val previous = field
            if (previous == value) return

            field = value

            val viewport = staticFrame?.viewport ?: return
            invalidateRects(
                previous?.let { entityRenderer.computeBoxRect(viewport, it) },
                value?.let { entityRenderer.computeBoxRect(viewport, it) },
            )
        }

    private val animationRunner =
        AnimationRunner(
            invalidateRects = { rects -> invalidateRects(*rects) },
            postDelayed = { runnable, delayMs -> postDelayed(runnable, delayMs) },
        )

    init {
        setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val viewport = staticFrame?.viewport ?: return@setOnTouchListener true
                val position =
                    viewport.screenToInnerCell(event.x, event.y)
                        ?: return@setOnTouchListener true
                onTapCell?.invoke(position)
            }
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawInternal(canvas)
    }

    fun setOnTapCell(handler: ((Position) -> Unit)?) {
        onTapCell = handler
    }

    fun applyEvent(event: GameRenderEvent) {
        when (event) {
            is GameRenderEvent.StateChanged -> {
                onStateChanged(
                    playerPosition = event.playerPosition,
                    boxPositions = event.boxPositions,
                    annotation = event.annotation,
                )
            }

            is GameRenderEvent.MoveRejected -> {
                onMoveRejected()
            }

            is GameRenderEvent.LevelSolvedWithCheat -> {
                onLevelSolvedWithCheat()
            }
        }
    }

    fun buildStaticBoardFrame(tileMap: TileMap): StaticBoardFrame {
        check(width > 0 && height > 0) { "GameBoardView must be laid out before loading a level" }

        val viewport =
            computeBoardViewport(
                surfaceWidth = width.toFloat(),
                surfaceHeight = height.toFloat(),
                innerRows = tileMap.rowCount,
                innerCols = tileMap.columnCount,
            )

        staticBoardRenderer.rebuildStaticLayout(
            viewWidth = width,
            viewHeight = height,
            viewport = viewport,
            tileMap = tileMap,
        )

        return StaticBoardFrame(
            bitmap = staticBoardRenderer.getStaticFrameBitmap(),
            viewport = viewport,
            tileMap = tileMap,
            width = width,
            height = height,
        )
    }

    fun loadLevel(
        tileMap: TileMap,
        playerPosition: Position,
        boxPositions: Set<Position>,
    ) {
        loadLevel(
            staticFrame = buildStaticBoardFrame(tileMap),
            playerPosition = playerPosition,
            boxPositions = boxPositions,
        )
    }

    fun loadLevel(
        staticFrame: StaticBoardFrame,
        playerPosition: Position,
        boxPositions: Set<Position>,
    ) {
        this.staticFrame = staticFrame
        entityRenderer.initGeometry(staticFrame.viewport)
        this.boxPositions = boxPositions
        this.playerPosition = playerPosition
        selectedBox = null
        invalidate()
    }

    private fun drawInternal(canvas: Canvas) {
        val playerPos = playerPosition ?: return
        val frame = staticFrame ?: return
        val viewport = frame.viewport

        canvas.drawBitmap(frame.bitmap, 0f, 0f, null)

        animationRunner.drawUnderEntities(canvas)

        entityRenderer.drawBoxes(
            canvas = canvas,
            viewport = viewport,
            boxPositions = boxPositions,
            selectedBox = selectedBox,
        )

        if (!animationRunner.hidesPlayer()) {
            entityRenderer.drawPlayer(
                canvas = canvas,
                viewport = viewport,
                playerPosition = playerPos,
            )
        }

        animationRunner.drawOverEntities(canvas)
    }

    private fun onStateChanged(
        playerPosition: Position,
        boxPositions: Set<Position>,
        annotation: GameRenderEvent.StateChangeAnnotation?,
    ) {
        val viewport = staticFrame!!.viewport
        val previousPlayer = this.playerPosition!!
        val previousBoxes = this.boxPositions
        val movedBoxes = previousBoxes - boxPositions
        val playerChanged = previousPlayer != playerPosition

        this.playerPosition = playerPosition
        this.boxPositions = boxPositions
        selectedBox = null

        val addedBoxes = boxPositions - previousBoxes
        invalidateRects(
            entityRenderer.computePlayerRect(viewport, playerPosition),
            *addedBoxes.map { entityRenderer.computeBoxRect(viewport, it) }.toTypedArray(),
        )

        if (movedBoxes.isNotEmpty() || playerChanged) {
            animationRunner.enqueue(
                EntityFlashAnimation(
                    renderer = entityRenderer,
                    viewport = viewport,
                    playerPosition = previousPlayer,
                    boxPositions = movedBoxes.toList(),
                ),
            )
        }

        when (annotation) {
            is GameRenderEvent.StateChangeAnnotation.BoxMoved -> {
                onBoxMoved(annotation.path)
            }

            is GameRenderEvent.StateChangeAnnotation.BoxRemoved -> {
                onBoxRemoved(annotation.position)
            }

            else -> {}
        }
    }

    private fun onBoxMoved(path: List<Position>) {
        if (path.size > 2) {
            val viewport = staticFrame!!.viewport
            animationRunner.enqueue(BoxPathAnimation(viewport, path))
        }
    }

    private fun onBoxRemoved(removedPosition: Position) {
        val viewport = staticFrame!!.viewport
        animationRunner.enqueue(BoxVanishAnimation(entityRenderer, viewport, removedPosition))
        animationRunner.enqueue(BlinkAnimation(entityRenderer, viewport, this.playerPosition!!))
    }

    private fun onMoveRejected() {
        val viewport = staticFrame!!.viewport
        val playerPos = playerPosition!!

        animationRunner.enqueue(BlinkAnimation(entityRenderer, viewport, playerPos))
    }

    private fun onLevelSolvedWithCheat() {
        val viewport = staticFrame!!.viewport
        val playerPos = playerPosition!!

        animationRunner.enqueue(BlinkAnimation(entityRenderer, viewport, playerPos))
    }

    private fun invalidateRects(vararg rects: Rect?) {
        val nonNull = rects.filterNotNull()
        if (nonNull.isEmpty()) return

        val dirty = Rect(nonNull[0])
        for (i in 1 until nonNull.size) {
            dirty.union(nonNull[i])
        }
        invalidateRectOnAnimation(dirty)
    }

    private fun invalidateRectOnAnimation(rect: Rect) {
        postInvalidateOnAnimation(rect.left, rect.top, rect.right, rect.bottom)
    }
}
