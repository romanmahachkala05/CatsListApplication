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
 * [value], except that once it turns true it stays true for at least [minimumMillis]. For
 * loading indicators that would otherwise flicker. A floor, not a delay: true still shows
 * immediately.
 */
@Composable
fun heldAtLeast(value: Boolean, minimumMillis: Long): Boolean {
    var held by remember { mutableStateOf(value) }
    var shownAtMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(value) {
        if (value) {
            // Monotonic on purpose: an NTP sync can step the wall clock backwards.
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
