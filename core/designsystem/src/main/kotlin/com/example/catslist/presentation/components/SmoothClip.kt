package com.example.catslist.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Clips to [shape] with antialiasing, which `Modifier.clip` does not do for an arbitrary path.
 *
 * `clip` hands the shape to the render node, and hardware clipping of a generic path is a hard
 * per-pixel in-or-out test — invisible on a straight edge, but on a curve it steps.
 *
 * So this masks instead: the content is composited offscreen, and then everything outside the
 * shape is erased with [BlendMode.Clear]. The erase goes through the ordinary path rasteriser,
 * which *is* antialiased, so the shape's soft edge becomes the content's soft edge.
 *
 * What is erased is built as `bounds − shape`, and drawing that is not the same as drawing the
 * shape itself in [BlendMode.DstIn] to keep the inside. A blend only affects the pixels its
 * draw actually covers, so `DstIn` on the shape leaves anything the shape does not reach
 * untouched — which quietly preserves a concave bite like this card's notch while still
 * trimming the corners, since those lie against the bounds. Naming the region to erase, rather
 * than the region to keep, removes the ambiguity.
 *
 * The cost is one offscreen layer per use, so this is for shapes whose edges are actually seen,
 * not a drop-in for every `clip` in the app.
 */
fun Modifier.smoothClip(shape: Shape): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val kept = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        val bounds = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val erased = Path().apply { op(bounds, kept, PathOperation.Difference) }
        onDrawWithContent {
            drawContent()
            // The colour is irrelevant — Clear only reads this shape's coverage.
            drawPath(path = erased, color = Color.Black, blendMode = BlendMode.Clear)
        }
    }
