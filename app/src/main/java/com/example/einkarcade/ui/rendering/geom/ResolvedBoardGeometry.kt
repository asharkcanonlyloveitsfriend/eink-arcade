package com.example.einkarcade.ui.rendering.geom

import android.graphics.Rect
import com.example.einkarcade.R
import com.example.einkarcade.sokoban.Position
import com.example.einkarcade.ui.rendering.AndroidGameAssets

/**
 * Immutable, resolved geometry derived from the current BoardViewport.
 * Valid until the level layout or surface size changes.
 */
class ResolvedBoardGeometry(
    val playerEyesOpaqueBoundsPx: Rect
) {

    companion object {
        private val playerEyesOpaqueBoundsCache = mutableMapOf<Int, Rect>()

        /**
         * Compute all resolved board geometry derived solely from tile size.
         * This must be called only when the tile size changes.
         */
        internal fun compute(
            tileSizePx: Float,
            assets: AndroidGameAssets
        ): ResolvedBoardGeometry {
            return ResolvedBoardGeometry(
                playerEyesOpaqueBoundsPx = computePlayerEyesOpaqueBounds(
                    tileSizePx = tileSizePx,
                    assets = assets
                )
            )
        }

        /**
         * Compute the opaque bounds of the player's eyes, relative to the
         * tile origin (0,0), in pixel coordinates.
         */
        private fun computePlayerEyesOpaqueBounds(
            tileSizePx: Float,
            assets: AndroidGameAssets
        ): Rect {
            val sizePx = snapToWholePixel(tileSizePx * 0.80f).toInt().coerceAtLeast(1)
            val cached = playerEyesOpaqueBoundsCache[sizePx]
            if (cached != null) {
                return Rect(cached)
            }
            val computed = Rect(
                assets.getOpaqueBounds(
                    R.drawable.player_eyes_open,
                    sizePx
                )
            )
            playerEyesOpaqueBoundsCache[sizePx] = Rect(computed)
            return computed
        }
    }
}
