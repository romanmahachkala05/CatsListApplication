package com.example.catslist.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.example.catslist.core.designsystem.R
import com.example.catslist.presentation.theme.successColor
import kotlinx.coroutines.delay

/** What the indicator is saying, in the order one refresh moves through them. */
private enum class RefreshPhase { Idle, Refreshing, Succeeded, Failed }

/**
 * Pull-to-refresh that reports how the refresh went, rather than just vanishing.
 *
 * The indicator does not snap back on release: it becomes a spinner for as long as the request
 * runs, then a tick or a cross, and only then retracts. A refresh that fails is otherwise
 * indistinguishable from one that returned the same cats — the list simply sits there, and the
 * user is left guessing whether anything happened.
 *
 * Both the spinner and the result are held briefly ([PHASE_MINIMUM_MILLIS]) so a fast response
 * still reads as a sequence rather than as a flicker.
 *
 * @param signal what the caller's request is doing.
 * @param topInset where the top of the screen effectively is. Content here is edge-to-edge and
 *   scrolls under the status bar, so without this the indicator rests behind it and is revealed
 *   already half-hidden. The lists pass the same inset they use as `contentPadding`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatPullToRefresh(
    signal: RefreshSignal,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val phase = rememberRefreshPhase(signal)
    val state = rememberPullToRefreshState()
    val isBusy = phase != RefreshPhase.Idle

    // One value for both the drag and the settle, rather than a dragged one and an animated one
    // swapped between. Two would mean the animation starts wherever it happens to be rather
    // than where the finger left off — on release the indicator would drop to the top and slide
    // back down to the position it was already at.
    val pulledOffset = CONTENT_OFFSET * state.distanceFraction.coerceIn(0f, 1f)
    val animatedOffset = remember { Animatable(0.dp, Dp.VectorConverter) }
    LaunchedEffect(isBusy, pulledOffset) {
        when {
            // Continues from wherever the drag ended, so release is a settle, not a jump.
            isBusy -> animatedOffset.animateTo(CONTENT_OFFSET)
            // Following the finger has to be exact; an animation here reads as lag.
            pulledOffset > 0.dp -> animatedOffset.snapTo(pulledOffset)
            else -> animatedOffset.animateTo(0.dp)
        }
    }
    val offset = animatedOffset.value

    // A result has to outlive its own phase. `phase` returns to Idle the moment the hold
    // expires, but the indicator is still on screen sliding away — and Idle draws the pull
    // arrow, so the tick would flip to an arrow and spin all the way out.
    //
    // The test for "still on its way out" is the indicator's own position, not the pull
    // distance: `distanceFraction` stays near 1 while the refresh runs and only decays
    // afterwards, so it never reads as released at the moment the result appears. Clearing on
    // arrival is what keeps the next pull showing an arrow rather than the previous outcome.
    var lastOutcome by remember { mutableStateOf(RefreshPhase.Idle) }
    val hasSettled = offset < SETTLED_THRESHOLD
    LaunchedEffect(phase, hasSettled) {
        if (phase != RefreshPhase.Idle) {
            lastOutcome = phase
        } else if (hasSettled) {
            lastOutcome = RefreshPhase.Idle
        }
    }
    val shownPhase = if (phase == RefreshPhase.Idle) lastOutcome else phase

    // The indicator travels between two positions rather than being pushed down from one.
    // Parking it at `topInset - size` does not hide it: the status bar is transparent under
    // edge-to-edge, so anything drawn behind it is simply visible, and at a 48dp inset a 40dp
    // circle sits 8dp *below* the top edge — permanently on screen. Hidden has to mean fully
    // above the screen, and only the destination is inset.
    val progress = if (CONTENT_OFFSET > 0.dp) (offset / CONTENT_OFFSET).coerceIn(0f, 1f) else 0f
    val indicatorY = lerp(-INDICATOR_SIZE, topInset + INDICATOR_MARGIN, progress)

    Box(
        modifier = modifier.pullToRefresh(
            isRefreshing = isBusy,
            state = state,
            onRefresh = onRefresh,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationY = offset.toPx() }) {
            content()
        }
        RefreshIndicator(
            phase = shownPhase,
            pullFraction = state.distanceFraction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                // Revealed by the same movement that pushes the list down, rather than fading
                // in over it.
                .graphicsLayer { translationY = indicatorY.toPx() },
        )
    }
}

/**
 * Drives [RefreshPhase] from [signal], holding each step long enough to be read.
 *
 * Success is inferred from a run that ended without [RefreshSignal.Failed] rather than being
 * signalled on its own, because the outcome only becomes readable once the request stops.
 */
@Composable
private fun rememberRefreshPhase(signal: RefreshSignal): RefreshPhase {
    var phase by remember { mutableStateOf(RefreshPhase.Idle) }
    LaunchedEffect(signal) {
        if (signal == RefreshSignal.Running) {
            phase = RefreshPhase.Refreshing
            delay(PHASE_MINIMUM_MILLIS)
        } else if (phase == RefreshPhase.Refreshing) {
            phase = if (signal == RefreshSignal.Failed) RefreshPhase.Failed else RefreshPhase.Succeeded
            delay(PHASE_MINIMUM_MILLIS)
            phase = RefreshPhase.Idle
        }
    }
    return phase
}

@Composable
private fun RefreshIndicator(phase: RefreshPhase, pullFraction: Float, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(INDICATOR_SIZE),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = INDICATOR_ELEVATION,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (phase) {
                // The arrow turns as you pull, so the gesture has somewhere to arrive.
                RefreshPhase.Idle -> Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = stringResource(R.string.common_cd_pull_to_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(ICON_SIZE)
                        .rotate(pullFraction.coerceIn(0f, 1f) * HALF_TURN),
                )
                RefreshPhase.Refreshing -> Spinner()
                RefreshPhase.Succeeded -> Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.common_cd_refresh_succeeded),
                    tint = successColor,
                    modifier = Modifier.size(ICON_SIZE),
                )
                RefreshPhase.Failed -> Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.common_cd_refresh_failed),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
        }
    }
}

/**
 * Spun by hand rather than left to [CircularProgressIndicator]'s own animation, so the arrow
 * that preceded it and this share one sense of rotation instead of one easing into the other.
 */
@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPINNER_MILLIS, easing = LinearEasing),
        ),
        label = "spinnerAngle",
    )
    CircularProgressIndicator(
        modifier = Modifier.size(ICON_SIZE).rotate(angle),
        strokeWidth = SPINNER_STROKE,
        color = MaterialTheme.colorScheme.primary,
    )
}

private val INDICATOR_SIZE = 40.dp
private val INDICATOR_MARGIN = 8.dp

/** Near enough to home to count as arrived; an animation's tail need not reach exactly zero. */
private val SETTLED_THRESHOLD = 1.dp
private val ICON_SIZE = 22.dp
private val INDICATOR_ELEVATION = 4.dp
private val CONTENT_OFFSET = 72.dp
private val SPINNER_STROKE = 2.5.dp
private const val PHASE_MINIMUM_MILLIS = 300L
private const val SPINNER_MILLIS = 900
private const val HALF_TURN = 180f
private const val FULL_TURN = 360f
