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
import com.example.catslist.presentation.downloadCat
import com.example.catslist.presentation.launchCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class CatsListViewModel @Inject constructor(
    getCatFeed: GetCatFeedUseCase,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel() {

    /**
     * Paging owns loading, error and retry for this stream itself — see
     * [androidx.paging.compose.LazyPagingItems.loadState] where it's collected. There is no
     * `CatsListState` any more: a custom Loading/Error/Content status would just be
     * reimplementing what `collectAsLazyPagingItems()` already tracks.
     */
    val pagedCats: Flow<PagingData<Cat>> = getCatFeed().cachedIn(viewModelScope)

    /**
     * The paged cats never carry favorite status themselves (see `CatRepositoryImpl.feed`'s
     * doc). The screen overlays it at render time by checking a cat's id against this set —
     * ordinary Compose recomposition, not another Paging generation.
     */
    val favoriteIds: StateFlow<Set<String>> = getFavoriteCats()
        .map { favorites -> favorites.mapTo(hashSetOf()) { it.id } }
        // Eagerly, not WhileSubscribed: this is a cheap derived set of already-cached Room
        // ids, not worth tying to whether a Composable happens to be collecting it right now.
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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
