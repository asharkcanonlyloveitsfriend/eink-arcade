package com.example.einkarcade.ui.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile

@SuppressLint("ClickableViewAccessibility")
internal class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var scene: GameScene? = null
    private var isGameWon: Boolean = false
    private var onTapCell: ((Position) -> Unit)? = null
    private var lastViewport: BoardViewport? = null
    private val assets = AndroidGameAssets(context)
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY }
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        holder.addCallback(this)
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (isGameWon) return@setOnTouchListener true
                val viewport = requireNotNull(lastViewport) {
                    "SurfaceView tap received before viewport was initialized."
                }
                val position = viewport.screenToInnerCell(event.x, event.y)
                if (position != null) {
                    onTapCell?.invoke(position)
                }
                return@setOnTouchListener true
            }
            true
        }
    }

    fun setContent(scene: GameScene, isGameWon: Boolean, onTapCell: (Position) -> Unit) {
        this.scene = scene
        this.isGameWon = isGameWon
        this.onTapCell = onTapCell
        render()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (scene != null) {
            render()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (scene != null) {
            render()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

    private fun render() {
        if (width <= 0 || height <= 0) return
        val scene = scene ?: return
        if (scene.tiles.isEmpty()) return
        if (scene.tiles.first().isEmpty()) return

        val innerRows = scene.tiles.size
        val innerCols = scene.tiles.first().size
        val viewport = computeBoardViewport(width.toFloat(), height.toFloat(), innerRows, innerCols)
        lastViewport = viewport

        if (!holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            val bitmapPaint = assets.bitmapPaint()
            pathPaint.strokeWidth = viewport.cellSize * 0.2f

            val cellSize = viewport.cellSize
            val offsetX = viewport.offsetX
            val offsetY = viewport.offsetY

            for ((rowIndex, row) in scene.tiles.withIndex()) {
                for ((colIndex, tile) in row.withIndex()) {
                    val point = Position(rowIndex + 1, colIndex + 1)
                        .toRenderPoint(cellSize, offsetX, offsetY)
                    val left = point.x
                    val top = point.y
                    val right = left + cellSize
                    val bottom = top + cellSize
                    when (tile) {
                        Tile.WALL -> canvas.drawRect(left, top, right, bottom, wallPaint)
                        Tile.FLOOR -> canvas.drawRect(left, top, right, bottom, floorPaint)
                        Tile.GOAL -> {
                            canvas.drawRect(left, top, right, bottom, floorPaint)
                            val centerX = left + cellSize / 2f
                            val centerY = top + cellSize / 2f
                            canvas.drawCircle(centerX, centerY, cellSize * 0.2f, goalPaint)
                        }
                    }
                }
            }

            for (position in scene.boxPositions) {
                val origin = Position(position.row + 1, position.col + 1)
                    .toRenderPoint(cellSize, offsetX, offsetY)
                val targetSize = snapToWholePixel(cellSize * 0.90f)
                val sizePx = targetSize.toInt()
                require(sizePx > 0)
                val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
                val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
                val resId =
                    if (scene.selectedBox == position) R.drawable.box_selected else R.drawable.box
                val bitmap = assets.getBitmap(resId, sizePx)
                canvas.drawBitmap(bitmap, left, top, bitmapPaint)
            }

            val origin = Position(scene.playerPosition.row + 1, scene.playerPosition.col + 1)
                .toRenderPoint(cellSize, offsetX, offsetY)
            val targetSize = snapToWholePixel(cellSize * 0.80f)
            val sizePx = targetSize.toInt()
            require(sizePx > 0)
            val left = snapToWholePixel(origin.x + (cellSize - targetSize) / 2f)
            val top = snapToWholePixel(origin.y + (cellSize - targetSize) / 2f)
            val body = assets.getBitmap(R.drawable.player_slime, sizePx)
            val eyesRes =
                if (scene.isBlinking) R.drawable.player_eyes_blink else R.drawable.player_eyes_open
            val eyes = assets.getBitmap(eyesRes, sizePx)
            drawSprite(canvas, body, left, top, sizePx, scene.isFacingLeft, bitmapPaint)
            drawSprite(canvas, eyes, left, top, sizePx, scene.isFacingLeft, bitmapPaint)

            if (scene.boxPathActive && scene.boxPath.size >= 2) {
                val first = scene.boxPath.first()
                var prevX = offsetX + (first.col + 1) * cellSize + cellSize / 2f
                var prevY = offsetY + (first.row + 1) * cellSize + cellSize / 2f

                for (i in 1 until scene.boxPath.size) {
                    val position = scene.boxPath[i]
                    val x = offsetX + (position.col + 1) * cellSize + cellSize / 2f
                    val y = offsetY + (position.row + 1) * cellSize + cellSize / 2f
                    canvas.drawLine(prevX, prevY, x, y, pathPaint)
                    prevX = x
                    prevY = y
                }
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        bitmap: Bitmap,
        left: Float,
        top: Float,
        sizePx: Int,
        flipX: Boolean,
        paint: Paint
    ) {
        canvas.save()
        canvas.translate(left, top)
        if (flipX) {
            canvas.scale(-1f, 1f, sizePx / 2f, sizePx / 2f)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }
}
