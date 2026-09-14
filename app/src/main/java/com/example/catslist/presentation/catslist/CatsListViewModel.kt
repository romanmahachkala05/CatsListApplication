package com.example.catslist.presentation.catslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.launchCatching
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.FetchNextCatsUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CatsListViewModel @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
    private val errorHandler: ICatsListErrorHandler,
    getCatFeed: GetCatFeedUseCase,
    private val fetchNextCats: FetchNextCatsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(), StateOwner<CatsListState> by stateHolder {

    init {
        getCatFeed()
            .onEach(stateHolder::showContent)
            .launchIn(viewModelScope)
        loadMore()
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: CatsListEvent) {
        when (event) {
            CatsListEvent.LoadMore -> loadMore()

            // A failed toggle leaves the feed itself intact, so it's a Snackbar rather
            // than an error status — same treatment as the download below.
            is CatsListEvent.ToggleFavorite -> launchCatching(
                onFailure = { notifier.showMessage(FAVORITE_FAILED) },
            ) {
                toggleFavorite(event.cat)
            }

            is CatsListEvent.Download -> launchCatching(
                onFailure = { notifier.showMessage(DOWNLOAD_FAILED) },
            ) {
                downloadCatImage(event.cat)
                notifier.showMessage(DOWNLOAD_STARTED)
            }
        }
    }

    /** A feed that wouldn't load is something the screen has to stay in, so it goes to state. */
    private fun loadMore() = launchCatching(onFailure = errorHandler::onLoadFailure) {
        fetchNextCats()
    }

    private companion object {
        val FAVORITE_FAILED = UiText.Resource(R.string.common_favorite_failed_message)
        val DOWNLOAD_FAILED = UiText.Resource(R.string.common_download_failed_message)
        val DOWNLOAD_STARTED = UiText.Resource(R.string.common_download_started_message)
    }
}
