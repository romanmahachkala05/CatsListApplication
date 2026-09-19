package com.example.catslist.presentation.catslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.FAVORITE_FAILED
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.downloadCat
import com.example.catslist.presentation.launchCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
internal class CatsListViewModel @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
    private val errorHandler: ICatsListErrorHandler,
    getCatFeed: GetCatFeedUseCase,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(),
    StateOwner<CatsListState> by stateHolder {

    /**
     * The one thing on this screen that is not in [CatsListState], because it cannot be:
     * `LazyPagingItems` is built by the Composable collecting this, and Paging drives its own
     * loading, error and retry from there. See ADR-0023 for why that state machine stays
     * Paging's rather than being mirrored into the state holder.
     */
    val pagedCats: Flow<PagingData<Cat>> = getCatFeed().cachedIn(viewModelScope)

    init {
        getFavoriteCats()
            .map { favorites -> favorites.map { it.id }.toPersistentSet() }
            // Without this a throwing Room query would escape viewModelScope and kill the
            // process. No RetryableFlow as on the favorites screen: there the stream *is* the
            // screen, so a retry button has somewhere to live; here the feed renders on regardless
            // and the only honest recovery is reopening the screen.
            .catch { errorHandler.onFavoriteIdsFailure(it) }
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: CatsListEvent) {
        when (event) {
            // A failed toggle leaves the feed itself intact, so it's a Snackbar rather
            // than an error status — same treatment as the download below.
            is CatsListEvent.ToggleFavorite -> launchCatching(
                onFailure = { notifier.showMessage(FAVORITE_FAILED) },
            ) {
                toggleFavorite(event.cat)
            }

            is CatsListEvent.Download -> downloadCat(event.cat, downloadCatImage, notifier)
        }
    }
}
