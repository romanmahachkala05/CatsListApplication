package com.example.catslist.presentation.catslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.catslist.domain.model.Cat
import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.CatItemPlaceholder
import com.example.catslist.presentation.components.CatListPlaceholder
import com.example.catslist.presentation.components.CatPullToRefresh
import com.example.catslist.presentation.components.EmptyMessage
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.components.RefreshSignal
import com.example.catslist.presentation.heldAtLeast
import com.example.catslist.presentation.theme.CatsListTheme
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

/** The download/favorite Snackbar for the screen's ViewModel is collected by the app's shared
 * host (see MainActivity) so it survives a tab switch — not collected here. */
@Composable
fun CatsListScreen(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    // A public function can't take an internal type as a parameter, so hiltViewModel()'s
    // default lives on this private overload instead — CatsListViewModel stays internal.
    CatsListScreen(modifier = modifier, contentPadding = contentPadding, viewModel = hiltViewModel())
}

@Composable
private fun CatsListScreen(
    modifier: Modifier,
    contentPadding: PaddingValues,
    viewModel: CatsListViewModel,
) {
    val pagingItems = viewModel.pagedCats.collectAsLazyPagingItems()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatsListContent(
        pagingItems = pagingItems,
        state = state,
        onEvent = viewModel::onEvent,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun CatsListContent(
    pagingItems: LazyPagingItems<Cat>,
    onEvent: (CatsListEvent) -> Unit,
    modifier: Modifier = Modifier,
    state: CatsListState = CatsListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // Whether there are cats decides this, not the load state. A refresh error or spinner
        // only takes over the screen while there is nothing to show — once cats are up, a failed
        // reload is the append footer's problem (see CatsFeed), not a reason to blank them out.
        val isEmpty = heldAtLeast(pagingItems.itemCount == 0, SKELETON_MINIMUM_MILLIS)
        if (isEmpty) {
            EmptyFeed(
                refresh = pagingItems.loadState.refresh,
                onRetry = pagingItems::retry,
                contentPadding = contentPadding,
            )
        } else {
            CatsFeed(
                pagingItems = pagingItems,
                state = state,
                onEvent = onEvent,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * What the screen shows before it has a single cat.
 *
 * The default is the skeleton, not an empty list: on a cold start Room's `PagingSource` settles
 * to "not loading, nothing here" while the `RemoteMediator` is still waiting on the network, and
 * rendering the feed in that window flashes a blank screen. Only a load that has genuinely
 * finished and still found nothing is allowed to say so.
 */
@Composable
private fun EmptyFeed(refresh: LoadState, onRetry: () -> Unit, contentPadding: PaddingValues) {
    when (refresh) {
        is LoadState.Error -> ErrorMessage(
            message = UiText.Resource(R.string.catslist_error_loading_cats),
            onRetry = onRetry,
        )
        is LoadState.NotLoading if refresh.endOfPaginationReached ->
            EmptyMessage(UiText.Resource(R.string.catslist_empty_message))
        else -> CatListPlaceholder(contentPadding = contentPadding)
    }
}

/**
 * Pulling refreshes rather than prepends. `CatFeedRemoteMediator`'s REFRESH clears the cached
 * feed and refetches from page 0, so a pull means "different cats, from the top" and the old
 * ones are gone — which is the only thing it can mean here: TheCatAPI has no "newer than what I
 * have" signal to prepend against, which is why PREPEND is a permanent no-op (ADR-0023).
 */
@Composable
private fun CatsFeed(
    pagingItems: LazyPagingItems<Cat>,
    state: CatsListState,
    onEvent: (CatsListEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    // The mediator's refresh, not the combined one. Combined also turns Loading whenever Room
    // hands Paging a new PagingSource generation, which happens on every page the mediator
    // caches — so the indicator would appear on its own while simply scrolling. The mediator's
    // own state is the only one that means "we are talking to the network".
    val signal = when (pagingItems.loadState.mediator?.refresh) {
        is LoadState.Loading -> RefreshSignal.Running
        is LoadState.Error -> RefreshSignal.Failed
        else -> RefreshSignal.Idle
    }
    CatPullToRefresh(
        signal = signal,
        onRefresh = pagingItems::refresh,
        modifier = Modifier.fillMaxSize(),
        // The same inset the list below uses, so the indicator clears the status bar that the
        // cats themselves scroll under.
        topInset = contentPadding.calculateTopPadding(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            when (state.favoritesStatus) {
                CatsListFavoritesStatus.Live -> Unit
                // A notice above the cats, not in place of them: the feed loaded fine, and only
                // the star icons are stale. Blanking working content over that is a worse lie.
                CatsListFavoritesStatus.Unavailable -> item {
                    ListNotice {
                        Text(
                            text = stringResource(R.string.catslist_error_favorites_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            items(count = pagingItems.itemCount, key = pagingItems.itemKey { it.id }) { index ->
                val cat = pagingItems[index] ?: return@items
                // The paged cat never carries favorite status itself — see CatRepositoryImpl's
                // feed doc — so it's applied here, at render time, a plain Compose
                // recomposition rather than another Paging generation.
                val displayCat = cat.copy(isFavorite = cat.id in state.favoriteIds)
                CatItem(
                    cat = displayCat,
                    onFavoriteClick = { onEvent(CatsListEvent.ToggleFavorite(displayCat)) },
                    onDownloadClick = { onEvent(CatsListEvent.Download(displayCat)) },
                )
            }

            when (pagingItems.loadState.append) {
                // The next card's own skeleton rather than a spinner below the list, so the page
                // arriving swaps shimmer for photo in place instead of shifting everything up.
                is LoadState.Loading -> item { CatItemPlaceholder() }
                is LoadState.Error -> item {
                    ListNotice {
                        Text(
                            text = stringResource(R.string.catslist_error_loading_cats),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = pagingItems::retry) {
                            Text(text = stringResource(R.string.catslist_action_retry))
                        }
                    }
                }
                is LoadState.NotLoading -> Unit
            }
        }
    }
}

/** Compact, in-list replacement for [ErrorMessage] — it `fillMaxSize()`s, which inside a
 * `LazyColumn` item takes the whole remaining viewport instead of sizing to its content. Used
 * both above the cats and as the append-failure footer below them. */
@Composable
private fun ListNotice(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/** Long enough that a skeleton reads as loading rather than as a flicker, short enough not to
 * slow down a genuinely fast page. */
private const val SKELETON_MINIMUM_MILLIS = 300L

@Preview(name = "Content", showBackground = true)
@Composable
private fun CatsListContentPreview() {
    CatsListTheme {
        val cats = listOf(
            Cat(id = "1", url = "", width = 300, height = 300),
            Cat(id = "2", url = "", width = 300, height = 300),
        )
        val pagingItems = flowOf(PagingData.from(cats)).collectAsLazyPagingItems()
        CatsListContent(
            pagingItems = pagingItems,
            state = CatsListState(favoriteIds = persistentSetOf("2")),
            onEvent = {},
        )
    }
}

@Preview(name = "Favorites unavailable", showBackground = true)
@Composable
private fun CatsListFavoritesUnavailablePreview() {
    CatsListTheme {
        val cats = listOf(Cat(id = "1", url = "", width = 300, height = 300))
        val pagingItems = flowOf(PagingData.from(cats)).collectAsLazyPagingItems()
        CatsListContent(
            pagingItems = pagingItems,
            state = CatsListState(favoritesStatus = CatsListFavoritesStatus.Unavailable),
            onEvent = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun CatsListLoadingPreview() {
    CatsListTheme {
        val loadingStates = LoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        val emptyFeed = flowOf(PagingData.from(emptyList<Cat>(), sourceLoadStates = loadingStates))
        CatsListContent(pagingItems = emptyFeed.collectAsLazyPagingItems(), onEvent = {})
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun CatsListErrorPreview() {
    CatsListTheme {
        val errorStates = LoadStates(
            refresh = LoadState.Error(IllegalStateException("preview")),
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        val emptyFeed = flowOf(PagingData.from(emptyList<Cat>(), sourceLoadStates = errorStates))
        val pagingItems = emptyFeed.collectAsLazyPagingItems()
        CatsListContent(pagingItems = pagingItems, onEvent = {})
    }
}
