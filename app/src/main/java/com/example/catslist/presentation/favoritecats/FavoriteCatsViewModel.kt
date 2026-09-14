package com.example.catslist.presentation.favoritecats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.launchCatching
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class FavoriteCatsViewModel @Inject constructor(
    private val stateHolder: IFavoriteCatsStateHolder,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(), StateOwner<FavoriteCatsState> by stateHolder {

    init {
        getFavoriteCats()
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: FavoriteCatsEvent) {
        when (event) {
            is FavoriteCatsEvent.RemoveFavorite -> launchCatching(
                onFailure = { notifier.showMessage(FAVORITE_FAILED) },
            ) {
                removeFavorite(event.cat)
            }

            is FavoriteCatsEvent.Download -> launchCatching(
                onFailure = { notifier.showMessage(DOWNLOAD_FAILED) },
            ) {
                downloadCatImage(event.cat)
                notifier.showMessage(DOWNLOAD_STARTED)
            }
        }
    }

    private companion object {
        val FAVORITE_FAILED = UiText.Resource(R.string.common_favorite_failed_message)
        val DOWNLOAD_FAILED = UiText.Resource(R.string.common_download_failed_message)
        val DOWNLOAD_STARTED = UiText.Resource(R.string.common_download_started_message)
    }
}
