package com.example.catslist.presentation.favoritecats

import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

internal interface IFavoriteCatsErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoritesFailure(error: Throwable)
}

@ViewModelScoped
internal class FavoriteCatsErrorHandler @Inject constructor(
    private val stateHolder: IFavoriteCatsStateHolder,
) : IFavoriteCatsErrorHandler {

    override fun onFavoritesFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.favoritecats_error_loading_favorites))
    }
}
