package com.example.catslist.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The sweep shown wherever something is still loading.
 *
 * The gradient's *end* is what animates, not its position: the band stretches out from the
 * origin rather than sliding across, which is what gives this its slow wash instead of a hard
 * glint. The greys are translucent on purpose, so the sweep reads against whatever sits behind
 * it in either theme.
 */
@Composable
internal fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = TRANSLATE_TO,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = SHIMMER_COLORS,
        start = Offset.Zero,
        end = Offset(x = translate, y = 0f),
    )
}

private val SHIMMER_COLORS = listOf(
    Color.LightGray.copy(alpha = 0.4f),
    Color.LightGray.copy(alpha = 0.2f),
    Color.LightGray.copy(alpha = 0.4f),
)
private const val TRANSLATE_TO = 1000f
private const val DURATION_MILLIS = 800
