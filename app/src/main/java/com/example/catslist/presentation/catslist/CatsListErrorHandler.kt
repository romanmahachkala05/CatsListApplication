package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

interface ICatsListErrorHandler {
    /** A page failed to load. The feed is still live, so the user can ask for it again. */
    fun onLoadFailure(error: Throwable)

    /** The feed stream itself ended in an error and won't emit again — retrying can't help. */
    fun onFeedFailure(error: Throwable)
}

@ViewModelScoped
class CatsListErrorHandler @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onLoadFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.catslist_error_loading_cats), retryable = true)
    }

    override fun onFeedFailure(error: Throwable) {
        // Offering Retry here would be a dead button: a Flow that threw is terminated, so
        // fetching another page would update a feed nothing is collecting any more.
        stateHolder.showError(UiText.Resource(R.string.catslist_error_feed_stopped), retryable = false)
    }
}
