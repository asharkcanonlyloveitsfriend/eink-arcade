package com.example.einkarcade.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.einkarcade.sokoban.Position

const val CELL_SIZE = 100f
const val GRID_OFFSET_X = 50f
const val GRID_OFFSET_Y = 50f

fun DrawScope.drawGameObject(
    position: Position,
    draw: DrawScope.(Offset) -> Unit
) {
    this.draw(position.toOffset())
}

fun DrawScope.drawWall(position: Position) {
    drawGameObject(position) { offset ->
        drawRect(
            color = Color.Black,
            topLeft = offset,
            size = Size(CELL_SIZE, CELL_SIZE)
        )
    }
}

fun DrawScope.drawFloor(position: Position) {
    drawGameObject(position) { offset ->
        drawRect(
            color = Color.White,
            topLeft = offset,
            size = Size(CELL_SIZE, CELL_SIZE)
        )
    }
}

fun DrawScope.drawGoal(position: Position) {
    drawGameObject(position) { offset ->
        drawRect(
            color = Color.LightGray,
            topLeft = offset,
            size = Size(CELL_SIZE, CELL_SIZE)
        )
    }
}

fun DrawScope.drawBox(position: Position, selected: Boolean) {
    val padding = CELL_SIZE * 0.2f
    val color = if (selected) Color.Black else Color.Gray
    drawGameObject(position) { offset ->
        drawRect(
            color = color,
            topLeft = Offset(offset.x + padding, offset.y + padding),
            size = Size(
                CELL_SIZE - 2 * padding,
                CELL_SIZE - 2 * padding
            )
        )
    }
}

fun DrawScope.drawPlayer(position: Position) {
    drawGameObject(position) { offset ->
        drawCircle(
            color = Color.Gray,
            radius = CELL_SIZE * 0.4f,
            center = Offset(
                offset.x + CELL_SIZE / 2,
                offset.y + CELL_SIZE / 2
            )
        )
    }
}

fun Position.toOffset(): Offset {
    return Offset(
        GRID_OFFSET_X + this.col * CELL_SIZE,
        GRID_OFFSET_Y + this.row * CELL_SIZE
    )
}
