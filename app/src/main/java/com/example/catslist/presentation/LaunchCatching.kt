package com.example.catslist.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs [block] in `viewModelScope` and routes a failure to [onFailure] instead of
 * letting it escape. An uncaught exception in `viewModelScope` reaches the default
 * handler, which on Android kills the process — so every event handler that calls
 * something fallible goes through here.
 *
 * [CancellationException] is rethrown, never reported: cancellation means the caller
 * went away, not that the work failed. Catching it would show the user an error for
 * leaving the screen, and would break structured concurrency for everything below.
 * This is why a bare `runCatching` is not good enough — it catches cancellation too.
 *
 * [onFailure] decides how the screen reacts. A failure the screen has to *stay* in
 * (a feed that wouldn't load) belongs in state via its `ErrorHandler`; a one-off
 * action that failed while the screen itself is still fine gets a Snackbar.
 */
fun ViewModel.launchCatching(onFailure: suspend (Throwable) -> Unit, block: suspend () -> Unit): Job =
    viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(this@launchCatching::class.simpleName, "Event handling failed", e)
            onFailure(e)
        }
    }
