package com.example.catslist.presentation.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A rounded card with a bite taken out of its bottom trailing corner, so the action icons sit in
 * the gap rather than on top of the cat.
 *
 * The outline is the card *minus* the notch rather than one hand-traced path: the subtraction
 * cannot disagree with itself the way two sets of arc coordinates can, and it keeps the notch
 * expressed in the same terms as the row that has to fit inside it.
 */
internal data class CatCardShape(
    private val corner: Dp,
    private val notchWidth: Dp,
    private val notchHeight: Dp,
    private val notchCorner: Dp,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        with(density) {
            val card = Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(corner.toPx())))
            }
            val notch = Path().apply { addRoundRect(notch(size, layoutDirection, this@with)) }
            Outline.Generic(Path().apply { op(card, notch, PathOperation.Difference) })
        }

    /**
     * Only the corner facing *into* the card is rounded. The notch's other three corners land on
     * the card's own edges, where the card's rounding already decides the silhouette.
     */
    private fun notch(size: Size, layoutDirection: LayoutDirection, density: Density): RoundRect =
        with(density) {
            val width = notchWidth.toPx()
            val height = notchHeight.toPx()
            val inner = CornerRadius(notchCorner.toPx())
            val bounds = when (layoutDirection) {
                LayoutDirection.Ltr -> Rect(size.width - width, size.height - height, size.width, size.height)
                LayoutDirection.Rtl -> Rect(0f, size.height - height, width, size.height)
            }
            when (layoutDirection) {
                LayoutDirection.Ltr -> RoundRect(
                    rect = bounds,
                    topLeft = inner,
                    topRight = CornerRadius.Zero,
                    bottomRight = CornerRadius.Zero,
                    bottomLeft = CornerRadius.Zero,
                )
                LayoutDirection.Rtl -> RoundRect(
                    rect = bounds,
                    topLeft = CornerRadius.Zero,
                    topRight = inner,
                    bottomRight = CornerRadius.Zero,
                    bottomLeft = CornerRadius.Zero,
                )
            }
        }
}
