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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.components.LoadingIndicator
import com.example.catslist.presentation.theme.CatsListTheme
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
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    CatsListContent(
        pagingItems = pagingItems,
        favoriteIds = favoriteIds,
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
    favoriteIds: Set<String> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        val refresh = pagingItems.loadState.refresh
        // A refresh error/spinner only takes over the whole screen while there is nothing
        // to show yet — once cats are on screen, a failed reload becomes the append footer's
        // problem (see CatsFeed), not a reason to blank out what's already loaded.
        when {
            refresh is LoadState.Loading && pagingItems.itemCount == 0 -> LoadingIndicator()
            refresh is LoadState.Error && pagingItems.itemCount == 0 -> ErrorMessage(
                message = UiText.Resource(R.string.catslist_error_loading_cats),
                onRetry = pagingItems::retry,
            )
            else -> CatsFeed(
                pagingItems = pagingItems,
                favoriteIds = favoriteIds,
                onEvent = onEvent,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun CatsFeed(
    pagingItems: LazyPagingItems<Cat>,
    favoriteIds: Set<String>,
    onEvent: (CatsListEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(count = pagingItems.itemCount, key = pagingItems.itemKey { it.id }) { index ->
            val cat = pagingItems[index] ?: return@items
            // The paged cat never carries favorite status itself — see CatRepositoryImpl.feed's
            // doc — so it's applied here, at render time, a plain Compose recomposition rather
            // than another Paging generation.
            val displayCat = cat.copy(isFavorite = cat.id in favoriteIds)
            CatItem(
                cat = displayCat,
                onFavoriteClick = { onEvent(CatsListEvent.ToggleFavorite(displayCat)) },
                onDownloadClick = { onEvent(CatsListEvent.Download(displayCat)) },
            )
        }

        when (pagingItems.loadState.append) {
            is LoadState.Loading -> item { AppendFooter { CircularProgressIndicator() } }
            is LoadState.Error -> item {
                AppendFooter {
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

/** Compact, list-footer replacement for [LoadingIndicator]/[ErrorMessage] — those
 * `fillMaxSize()`, which inside a `LazyColumn` item takes the whole remaining viewport
 * instead of sizing to its content. */
@Composable
private fun AppendFooter(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Preview(name = "Content", showBackground = true)
@Composable
private fun CatsListContentPreview() {
    CatsListTheme {
        val cats = listOf(
            Cat(id = "1", url = "", width = 300, height = 300),
            Cat(id = "2", url = "", width = 300, height = 300),
        )
        val pagingItems = flowOf(PagingData.from(cats)).collectAsLazyPagingItems()
        CatsListContent(pagingItems = pagingItems, favoriteIds = setOf("2"), onEvent = {})
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
