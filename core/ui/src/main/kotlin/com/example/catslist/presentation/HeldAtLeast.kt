package com.example.catslist.presentation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * [value], except that once it turns true it stays true for at least [minimumMillis].
 *
 * For loading indicators that would otherwise flicker. A cached or fast response can arrive
 * within a frame or two, and a placeholder that appears and vanishes that quickly reads as a
 * glitch rather than as loading. Holding it costs a fraction of a second on the fast path and
 * buys a screen that never flickers on the slow one.
 *
 * Note this is a floor, not a delay: the placeholder still appears the instant [value] is true.
 */
@Composable
fun heldAtLeast(value: Boolean, minimumMillis: Long): Boolean {
    var held by remember { mutableStateOf(value) }
    var shownAtMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(value) {
        if (value) {
            // Monotonic on purpose: the wall clock can step backwards on an NTP sync, which
            // would make the elapsed time negative and hold this for as long as the clock moved.
            shownAtMillis = SystemClock.elapsedRealtime()
            held = true
        } else {
            val remaining = minimumMillis - (SystemClock.elapsedRealtime() - shownAtMillis)
            if (remaining > 0) delay(remaining)
            held = false
        }
    }
    return value || held
}
