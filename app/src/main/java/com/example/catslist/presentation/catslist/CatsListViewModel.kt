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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

    /** Bumped to resubscribe to the feed. Its value carries no meaning beyond "different". */
    private val feedSubscriptions = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val feed = feedSubscriptions.flatMapLatest {
        // `catch` sits on the inner flow on purpose. Downstream of flatMapLatest it would
        // terminate the whole chain including the trigger, and the retry would be a dead
        // button — the bug this replaces. Here only the failed subscription ends.
        getCatFeed().catch { errorHandler.onFeedFailure(it) }
    }

    init {
        feed
            .onEach(stateHolder::showContent)
            .launchIn(viewModelScope)
        loadMore()
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: CatsListEvent) {
        when (event) {
            CatsListEvent.LoadMore -> loadMore()

            CatsListEvent.Retry -> retry()

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

    /**
     * Recovers from either failure with one action, so the screen needs no record of which
     * one it hit. Resubscribing to a feed that was healthy all along is cheap and loses
     * nothing — the cats live in the repository, not in the subscription.
     */
    private fun retry() {
        stateHolder.showLoading()
        feedSubscriptions.update { it + 1 }
        loadMore()
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
