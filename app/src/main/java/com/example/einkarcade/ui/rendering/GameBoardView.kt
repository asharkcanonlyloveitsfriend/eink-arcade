package com.example.einkarcade.ui.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.example.einkarcade.GameController
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.sokoban.Tile
import com.example.einkarcade.ui.rendering.draw.BackgroundDrawer
import com.example.einkarcade.ui.rendering.draw.EntityDrawer
import com.example.einkarcade.ui.rendering.draw.GameRenderer
import com.example.einkarcade.ui.rendering.draw.TileDrawer
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.ResolvedEntityGeometry
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import com.example.einkarcade.ui.rendering.geom.screenToInnerCell

@SuppressLint("ClickableViewAccessibility")
internal class GameBoardView(
    context: Context
) : View(context), GameSurface {

    private val assets = AndroidGameAssets(context)
    private val renderer = GameRenderer(
        backgroundDrawer = BackgroundDrawer(context),
        tileDrawer = TileDrawer(),
        entityDrawer = EntityDrawer(assets)
    )

    private var tiles: List<List<Tile>> = emptyList()
    private var boxPositions: Set<Position> = emptySet()
    private var playerPosition: Position? = null

    private var onTapCell: ((Position) -> Unit)? = null
    private var selectedBox: Position? = null

    private var lastViewport: BoardViewport? = null
    private var resolvedEntityGeometry: ResolvedEntityGeometry? = null

    init {
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

    override fun asView(): View = this

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        drawInternal(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        lastViewport = null
        resolvedEntityGeometry = null
        invalidate()
    }

    override fun setOnTapCell(handler: (Position) -> Unit) {
        onTapCell = handler
    }

    override fun getSelectedBox(): Position? = selectedBox

    override fun setSelectedBox(position: Position?) {
        val previous = selectedBox
        selectedBox = position

        val dirty = renderer.computeDirtyRect(
            viewport = lastViewport!!,
            geometry = resolvedEntityGeometry!!,
            boxPositions = listOfNotNull(previous, position)
        )

        postInvalidateOnAnimation(
            dirty.left,
            dirty.top,
            dirty.right,
            dirty.bottom
        )
    }

    override fun applyDelta(delta: GameController.RenderDelta) {
        when (delta) {
            is GameController.RenderDelta.LevelLoaded -> {
                onLevelLoaded(
                    tiles = delta.tiles,
                    boxPositions = delta.boxPositions,
                    playerPosition = delta.playerPosition
                )
            }
            is GameController.RenderDelta.PlayerMoved -> onPlayerMoved(to = delta.to)
            is GameController.RenderDelta.BoxMoved -> onBoxMoved(path = delta.path)
            is GameController.RenderDelta.MoveRejected -> Unit
            is GameController.RenderDelta.GameWon -> Unit
        }
    }

    private fun drawInternal(canvas: android.graphics.Canvas) {
        val playerPos = playerPosition ?: return
        if (tiles.isEmpty()) return
        if (width <= 0 || height <= 0) return

        val viewport = computeBoardViewport(
            surfaceWidth = width.toFloat(),
            surfaceHeight = height.toFloat(),
            innerRows = tiles.size,
            innerCols = tiles[0].size
        )
        lastViewport = viewport
        resolvedEntityGeometry = ResolvedEntityGeometry.compute(
            viewport.cellSize,
            assets = assets
        )

        renderer.rebuildStaticFrame(
            viewWidth = width,
            viewHeight = height,
            viewport = viewport,
            tiles = tiles
        )

        renderer.drawStaticFrame(
            canvas = canvas,
            viewWidth = width,
            viewHeight = height
        )

        val geometry = resolvedEntityGeometry ?: return
        renderer.drawEntities(
            canvas = canvas,
            viewport = viewport,
            geometry = geometry,
            boxPositions = boxPositions,
            playerPosition = playerPos,
            selectedBox = selectedBox
        )
    }

    private fun onLevelLoaded(
        tiles: List<List<Tile>>,
        boxPositions: Set<Position>,
        playerPosition: Position
    ) {
        this.tiles = tiles
        this.boxPositions = boxPositions
        this.playerPosition = playerPosition
        selectedBox = null
        invalidate()
    }

    private fun onPlayerMoved(to: Position) {
        val viewport = lastViewport!!
        val previous = playerPosition!!
        playerPosition = to

        val dirty = renderer.computeDirtyRect(
            viewport = viewport,
            geometry = resolvedEntityGeometry!!,
            playerPositions = listOf(previous, to)
        )

        postInvalidateOnAnimation(
            dirty.left,
            dirty.top,
            dirty.right,
            dirty.bottom
        )
    }

    private fun onBoxMoved(path: List<Position>) {
        val viewport = lastViewport!!
        val geometry = resolvedEntityGeometry!!
        val previousPlayer = playerPosition!!

        val boxFrom = path.first()
        val boxTo = path.last()
        val playerPosition = path[path.size - 2]

        boxPositions = boxPositions - boxFrom + boxTo

        val dirty = renderer.computeDirtyRect(
            viewport = viewport,
            geometry = geometry,
            boxPositions = listOf(boxFrom, boxTo),
            playerPositions = listOf(previousPlayer, playerPosition)
        )

        postInvalidateOnAnimation(
            dirty.left,
            dirty.top,
            dirty.right,
            dirty.bottom
        )
    }
}
