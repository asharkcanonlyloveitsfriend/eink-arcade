@file:Suppress("ktlint:standard:function-naming")

package com.example.einkarcade.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.einkarcade.R
import com.example.einkarcade.catalog.LevelBoardGeometry
import com.example.einkarcade.catalog.LevelSummary
import com.example.einkarcade.ui.PagedScrollIndicator
import com.example.einkarcade.ui.ShadowedIconButton
import com.example.einkarcade.ui.verticalPageSwipe
import com.example.einkarcade.ui.rendering.AndroidGameAssets
import com.example.einkarcade.ui.rendering.draw.BackgroundBitmapCache
import com.example.einkarcade.ui.rendering.draw.EntityDrawer
import com.example.einkarcade.ui.rendering.draw.TileDrawer
import com.example.einkarcade.ui.rendering.geom.ResolvedEntityGeometry
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import kotlin.math.roundToInt

// Page layout.
private const val PAGE_LEFT_MARGIN_DP = 24
private const val PAGE_VERTICAL_MARGIN_DP = 6
private const val RIGHT_SCROLL_RAIL_WIDTH_DP = 48
private const val SCROLLBAR_VERTICAL_MARGIN_DP = 18

@Composable
fun LevelPickerOverlay(
    levels: List<LevelSummary>,
    selectedPuzzleId: Int,
    onPickLevel: (puzzleId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }

    val selectedIndex = levels.indexOfFirst { it.puzzleId == selectedPuzzleId }
    val context = LocalContext.current
    val gameAssets = remember(context) { AndroidGameAssets(context) }
    val lastPageStartIndex = LevelPickerPaging.lastPageStart(levels.size)
    val initialPageStartIndex = LevelPickerPaging.initialPageStart(selectedIndex, levels.size)
    var pageStartIndex by
        remember(selectedIndex, lastPageStartIndex) {
            mutableIntStateOf(initialPageStartIndex)
        }
    val visibleLevels = levels.drop(pageStartIndex).take(LevelPickerPaging.PAGE_SIZE)
    val scrollFraction =
        if (lastPageStartIndex > 0) pageStartIndex.toFloat() / lastPageStartIndex else 0f
    val selectedScrollFraction =
        LevelPickerPaging.selectedRowScrollFraction(selectedIndex, levels.size)
    val visibleFraction =
        if (levels.isNotEmpty()) {
            (LevelPickerPaging.PAGE_SIZE.toFloat() / levels.size).coerceAtMost(1f)
        } else {
            1f
        }

    fun showPage(targetIndex: Int) {
        if (levels.isEmpty()) return
        pageStartIndex = targetIndex.coerceIn(0, lastPageStartIndex)
    }

    fun showPreviousPage() {
        showPage(LevelPickerPaging.previousPageStart(pageStartIndex))
    }

    fun showNextPage() {
        showPage(LevelPickerPaging.nextPageStart(pageStartIndex, levels.size))
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalPageSwipe(
                    onPreviousPage = { showPreviousPage() },
                    onNextPage = { showNextPage() },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val screenAspectRatio =
            if (containerHeight > 0.dp) {
                containerWidth.value / containerHeight.value
            } else {
                1f
            }
        val gridWidth =
            (containerWidth - (PAGE_LEFT_MARGIN_DP + RIGHT_SCROLL_RAIL_WIDTH_DP).dp)
                .coerceAtLeast(0.dp)
        val gridHeight =
            (containerHeight - (PAGE_VERTICAL_MARGIN_DP * 2).dp).coerceAtLeast(0.dp)

        // Preserve the screen aspect ratio, scaling down only if height is the tighter limit.
        val widthLimitedCardWidth = gridWidth / LevelPickerPaging.GRID_SIZE
        val heightLimitedCardWidth =
            (gridHeight / LevelPickerPaging.GRID_SIZE) * screenAspectRatio
        val cardWidth = minOf(widthLimitedCardWidth, heightLimitedCardWidth)
        val cardHeight = cardWidth / screenAspectRatio
        val rowSpacing =
            (
                (gridHeight - (cardHeight * LevelPickerPaging.GRID_SIZE)) /
                    (LevelPickerPaging.GRID_SIZE - 1)
            ).coerceAtLeast(0.dp)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val background =
                BackgroundBitmapCache.get(
                    context = context,
                    width = size.width.roundToInt(),
                    height = size.height.roundToInt(),
                )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(background, 0f, 0f, null)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = PAGE_LEFT_MARGIN_DP.dp,
                        end = RIGHT_SCROLL_RAIL_WIDTH_DP.dp,
                        top = PAGE_VERTICAL_MARGIN_DP.dp,
                        bottom = PAGE_VERTICAL_MARGIN_DP.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            repeat(LevelPickerPaging.GRID_SIZE) { row ->
                Row(modifier = Modifier.height(cardHeight)) {
                    repeat(LevelPickerPaging.GRID_SIZE) { column ->
                        val level =
                            visibleLevels.getOrNull(
                                (row * LevelPickerPaging.GRID_SIZE) + column,
                            )
                        if (level != null) {
                            val isSelected = level.puzzleId == selectedPuzzleId
                            LevelCard(
                                level = level,
                                isSelected = isSelected,
                                assets = gameAssets,
                                modifier = Modifier.size(cardWidth, cardHeight),
                                onClick = {
                                    if (!isSelected) onPickLevel(level.puzzleId)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }

        PagedScrollIndicator(
            scrollFraction = scrollFraction,
            visibleFraction = visibleFraction,
            selectedFraction = selectedScrollFraction,
            onScrollToStart = { showPage(0) },
            onScrollToEnd = { showPage(lastPageStartIndex) },
            onScrollToSelected = { showPage(initialPageStartIndex) },
            onPreviousPage = { showPreviousPage() },
            onNextPage = { showNextPage() },
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(RIGHT_SCROLL_RAIL_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .padding(vertical = SCROLLBAR_VERTICAL_MARGIN_DP.dp),
        )

        ShadowedIconButton(
            iconResource = R.drawable.ic_back,
            contentDescription = "Back",
            buttonSize = 52.dp,
            iconSize = 32.dp,
            onClick = onDismiss,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = 18.dp,
                        top = 12.dp,
                    ),
        )
    }
}

@Composable
private fun LevelCard(
    level: LevelSummary,
    isSelected: Boolean,
    assets: AndroidGameAssets,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(8.dp)

    Box(
        modifier =
            modifier
                .clip(cardShape)
                .clickable(onClick = onClick),
    ) {
        if (isSelected) {
            Canvas(
                modifier = Modifier.matchParentSize(),
            ) {
                drawRect(
                    color = Color.White.copy(alpha = 0.18f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Screen,
                )
            }
        }
        // Soft halo that will only show through VOID areas.
        if (level.isCompleted) {
            Canvas(
                modifier = Modifier.matchParentSize(),
            ) {
                val radius = size.minDimension * 0.91f
                drawCircle(
                    brush =
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFFDCDCDC).copy(alpha = 0.6375f),
                                    Color(0xFFDCDCDC).copy(alpha = 0.25f),
                                    Color(0xFFDCDCDC).copy(alpha = 0.0675f),
                                    Color.Transparent,
                                ),
                            center = center,
                            radius = radius,
                        ),
                )
            }
        }

        LevelMapPreview(
            board = level.boardGeometry,
            assets = assets,
            isSelected = isSelected,
            modifier = Modifier.fillMaxSize(),
        )

        // Large completion check drawn over the map.
        if (level.isCompleted) {
            Image(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "Completed",
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.22f)),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.5f),
            )
        }
    }
}

@Composable
private fun LevelMapPreview(
    board: LevelBoardGeometry,
    assets: AndroidGameAssets,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val tileDrawer = remember { TileDrawer(usePreviewStyle = true) }
    val entityDrawer = remember(assets) { EntityDrawer(assets) }
    Canvas(
        modifier = modifier,
    ) {
        val tileMap = board.tileMap
        if (
            tileMap.rowCount == 0 ||
            tileMap.columnCount == 0 ||
            size.width <= 0f ||
            size.height <= 0f
        ) {
            return@Canvas
        }

        val viewport =
            computeBoardViewport(
                surfaceWidth = size.width,
                surfaceHeight = size.height,
                innerRows = tileMap.rowCount,
                innerCols = tileMap.columnCount,
            )
        val entityGeometry = ResolvedEntityGeometry.compute(viewport.cellSize, assets)

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            tileDrawer.drawTiles(nativeCanvas, viewport, tileMap)
            entityDrawer.drawBoxes(nativeCanvas, viewport, entityGeometry, board.boxes)
            entityDrawer.drawPlayer(nativeCanvas, viewport, board.player, entityGeometry)
        }

        // Draw selection brackets only in the rounded corners.
        if (isSelected) {
            drawSelectionBrackets(
                color = Color.LightGray,
                strokeWidth = 6.dp.toPx(),
                radius = 8.dp.toPx(),
            )
        }
    }
}

private fun DrawScope.drawSelectionBrackets(
    color: Color,
    strokeWidth: Float,
    radius: Float,
) {
    drawSelectionCorner(color, strokeWidth, radius, isRight = false, isBottom = false)
    drawSelectionCorner(color, strokeWidth, radius, isRight = true, isBottom = false)
    drawSelectionCorner(color, strokeWidth, radius, isRight = true, isBottom = true)
    drawSelectionCorner(color, strokeWidth, radius, isRight = false, isBottom = true)
}

private fun DrawScope.drawSelectionCorner(
    color: Color,
    strokeWidth: Float,
    radius: Float,
    isRight: Boolean,
    isBottom: Boolean,
) {
    val cornerX = if (isRight) size.width else 0f
    val cornerY = if (isBottom) size.height else 0f
    val horizontalDirection = if (isRight) -1f else 1f
    val verticalDirection = if (isBottom) -1f else 1f
    val diameter = radius * 2f
    val extension = diameter * 0.75f
    val arcStartAngle =
        when {
            isRight && isBottom -> 0f
            !isRight && isBottom -> 90f
            !isRight -> 180f
            else -> 270f
        }

    drawArc(
        color = color,
        startAngle = arcStartAngle,
        sweepAngle = 90f,
        useCenter = false,
        topLeft =
            Offset(
                x = if (isRight) cornerX - diameter else cornerX,
                y = if (isBottom) cornerY - diameter else cornerY,
            ),
        size = Size(diameter, diameter),
        style = Stroke(width = strokeWidth),
    )

    val verticalStart = Offset(cornerX, cornerY + (verticalDirection * radius))
    drawLine(
        color = color,
        start = verticalStart,
        end = verticalStart.copy(y = verticalStart.y + (verticalDirection * extension)),
        strokeWidth = strokeWidth,
    )

    val horizontalStart = Offset(cornerX + (horizontalDirection * radius), cornerY)
    drawLine(
        color = color,
        start = horizontalStart,
        end = horizontalStart.copy(x = horizontalStart.x + (horizontalDirection * extension)),
        strokeWidth = strokeWidth,
    )
}
