package com.example.catslist.presentation.catslist

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

internal interface ICatsListErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoriteIdsFailure(error: Throwable)
}

/** Covers the favorite overlay only; a failed feed load is Paging's (ADR-0024). */
@ViewModelScoped
internal class CatsListErrorHandler @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onFavoriteIdsFailure(error: Throwable) {
        stateHolder.showFavoritesUnavailable()
    }
}
