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
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.theme.CatsListTheme
import kotlinx.collections.immutable.persistentSetOf
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
        val refresh = pagingItems.loadState.refresh
        // A refresh error/spinner only takes over the whole screen while there is nothing
        // to show yet — once cats are on screen, a failed reload becomes the append footer's
        // problem (see CatsFeed), not a reason to blank out what's already loaded.
        when (refresh) {
            is LoadState.Loading if pagingItems.itemCount == 0 -> CatsFeedPlaceholder(contentPadding)
            is LoadState.Error if pagingItems.itemCount == 0 -> ErrorMessage(
                message = UiText.Resource(R.string.catslist_error_loading_cats),
                onRetry = pagingItems::retry,
            )
            else -> CatsFeed(
                pagingItems = pagingItems,
                state = state,
                onEvent = onEvent,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun CatsFeed(
    pagingItems: LazyPagingItems<Cat>,
    state: CatsListState,
    onEvent: (CatsListEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        when (state.favoritesStatus) {
            CatsListFavoritesStatus.Live -> Unit
            // A notice above the cats, not in place of them: the feed loaded fine, and only the
            // star icons are stale. Blanking working content over that would be a worse lie.
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
            // The paged cat never carries favorite status itself — see CatRepositoryImpl.feed's
            // doc — so it's applied here, at render time, a plain Compose recomposition rather
            // than another Paging generation.
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

/**
 * The skeleton list shown before the first page arrives. Not scrollable: there is nothing below
 * it to reach, and a skeleton that moves invites the user to chase content that does not exist.
 */
@Composable
private fun CatsFeedPlaceholder(contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        items(PLACEHOLDER_COUNT) { CatItemPlaceholder() }
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

/** Enough to fill a phone screen and then some, so the skeleton never ends mid-viewport. */
private const val PLACEHOLDER_COUNT = 4

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
