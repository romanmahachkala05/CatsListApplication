package com.example.catslist.presentation.catslist

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

internal interface ICatsListErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoriteIdsFailure(error: Throwable)
}

/**
 * Note what this handler does *not* cover: a failed load of the paged feed. That one belongs to
 * Paging, which reports and retries it through `LazyPagingItems.loadState` (ADR-0023). Only the
 * favorite overlay's failure reaches state through here.
 */
@ViewModelScoped
internal class CatsListErrorHandler @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onFavoriteIdsFailure(error: Throwable) {
        stateHolder.showFavoritesUnavailable()
    }
}
