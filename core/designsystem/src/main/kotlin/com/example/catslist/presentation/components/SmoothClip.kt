package com.example.catslist.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Clips to [shape] with antialiasing, which `Modifier.clip` does not do for an arbitrary path.
 *
 * `clip` hands the shape to the render node, and hardware clipping of a generic path is a hard
 * per-pixel in-or-out test — on a straight edge that is invisible, but on a curve it steps. The
 * card's rounded corners and notch are all curve, so the whole silhouette shows it.
 *
 * Masking instead of clipping avoids that. The content is composited offscreen, then the shape
 * is drawn over it in [BlendMode.DstIn], which keeps the content only where the shape is opaque.
 * That drawing goes through the ordinary path rasteriser, which *is* antialiased, so the shape's
 * soft edge becomes the content's soft edge.
 *
 * The cost is one offscreen layer per use, so this is for shapes whose edges are actually seen,
 * not a drop-in for every `clip` in the app.
 */
fun Modifier.smoothClip(shape: Shape): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        val path = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        onDrawWithContent {
            drawContent()
            // The colour is irrelevant — DstIn reads only this shape's alpha.
            drawPath(path = path, color = Color.Black, blendMode = BlendMode.DstIn)
        }
    }
