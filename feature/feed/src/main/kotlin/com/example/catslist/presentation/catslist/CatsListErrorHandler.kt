package com.example.catslist.presentation.catslist

import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

internal interface ICatsListErrorHandler {
    /** A page failed to load. */
    fun onLoadFailure(error: Throwable)

    /** The feed stream ended in an error, so it needs resubscribing, not just another page. */
    fun onFeedFailure(error: Throwable)
}

@ViewModelScoped
internal class CatsListErrorHandler @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onLoadFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.catslist_error_loading_cats))
    }

    override fun onFeedFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.catslist_error_feed_stopped))
    }
}
