package com.example.catslist.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * A diagonal highlight sweeping across a placeholder.
 *
 * The band is sized in Dp rather than from the composable's own bounds, so placeholders that are
 * laid out one above the other share a single sweep travelling down the screen instead of each
 * card flashing on its own phase.
 */
@Composable
internal fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    // A step toward the contrasting colour rather than a fixed lighter one: in dark theme there
    // is no lighter surface to move toward, and a hardcoded highlight disappears entirely.
    val highlight = MaterialTheme.colorScheme.onSurface
        .copy(alpha = HIGHLIGHT_ALPHA)
        .compositeOver(base)
    val band = with(LocalDensity.current) { BAND.toPx() }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val start by transition.animateFloat(
        initialValue = -band,
        targetValue = band * TRAVEL_BANDS,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DURATION_MILLIS, easing = LinearEasing),
        ),
        label = "shimmerStart",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + band, band),
    )
}

private val BAND = 220.dp
private const val TRAVEL_BANDS = 3f
private const val DURATION_MILLIS = 1400
private const val HIGHLIGHT_ALPHA = 0.14f
