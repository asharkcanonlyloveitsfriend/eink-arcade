package com.example.einkarcade.ui.rendering.geom

import android.graphics.Rect
import com.example.einkarcade.R
import com.example.einkarcade.ui.rendering.AndroidGameAssets

/**
 * Immutable, resolved geometry derived from the current BoardViewport.
 * Valid until the level layout or surface size changes.
 */
class ResolvedEntityGeometry(
    val boxBoundsPx: Rect,
    val boxSizePx: Int,
    val playerEyesOpaqueBoundsPx: Rect,
    val playerBoundsPx: Rect,
    val playerSizePx: Int
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
        ): ResolvedEntityGeometry {
            val (boxSizePx, boxBoundsPx) = computeBoxGeometry(tileSizePx)
            val (playerSizePx, playerBoundsPx) = computePlayerGeometry(tileSizePx)

            return ResolvedEntityGeometry(
                boxBoundsPx = boxBoundsPx,
                boxSizePx = boxSizePx,
                playerEyesOpaqueBoundsPx = computePlayerEyesOpaqueBounds(
                    tileSizePx = tileSizePx,
                    assets = assets
                ),
                playerBoundsPx = playerBoundsPx,
                playerSizePx = playerSizePx
            )
        }

        private fun computeBoxGeometry(tileSizePx: Float): Pair<Int, Rect> {
            val boxSizePx = snapToWholePixel(tileSizePx * 0.90f)
                .toInt()
                .coerceAtLeast(1)

            val boxInsetPx = snapToWholePixel((tileSizePx - boxSizePx) / 2f)
                .toInt()

            val boxBoundsPx = Rect(
                boxInsetPx,
                boxInsetPx,
                boxInsetPx + boxSizePx,
                boxInsetPx + boxSizePx
            )

            return boxSizePx to boxBoundsPx
        }

        private fun computePlayerGeometry(tileSizePx: Float): Pair<Int, Rect> {
            val playerSizePx = snapToWholePixel(tileSizePx).toInt().coerceAtLeast(1)
            val insetPx = snapToWholePixel((tileSizePx - playerSizePx) / 2f).toInt()

            val boundsPx = Rect(
                insetPx,
                insetPx,
                insetPx + playerSizePx,
                insetPx + playerSizePx
            )

            return playerSizePx to boundsPx
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
