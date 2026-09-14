package com.example.catslist.presentation.favoritecats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.launchCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class FavoriteCatsViewModel @Inject constructor(
    private val stateHolder: IFavoriteCatsStateHolder,
    private val errorHandler: IFavoriteCatsErrorHandler,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(),
    StateOwner<FavoriteCatsState> by stateHolder {

    /** Bumped to resubscribe to the favorites stream. Its value carries no meaning. */
    private val subscriptions = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favorites = subscriptions.flatMapLatest {
        // Without this a throwing Room query would escape viewModelScope and kill the
        // process. `catch` rethrows the coroutine's own cancellation, so unlike
        // `runCatching` it needs no manual guard for that. It sits on the inner flow so
        // that a failure ends only this subscription, leaving the retry able to start
        // another one.
        getFavoriteCats().catch { errorHandler.onFavoritesFailure(it) }
    }

    init {
        favorites
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: FavoriteCatsEvent) {
        when (event) {
            FavoriteCatsEvent.Retry -> retry()

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

    private fun retry() {
        stateHolder.showLoading()
        subscriptions.update { it + 1 }
    }

    private companion object {
        val FAVORITE_FAILED = UiText.Resource(R.string.common_favorite_failed_message)
        val DOWNLOAD_FAILED = UiText.Resource(R.string.common_download_failed_message)
        val DOWNLOAD_STARTED = UiText.Resource(R.string.common_download_started_message)
    }
}
