package com.example.catslist.presentation.catslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catslist.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.components.LoadingIndicator
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The download/favorite Snackbar for [viewModel] is collected by the app's shared
 * host (see MainActivity) so it survives a tab switch — not collected here. */
@Composable
fun CatsListScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: CatsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatsListContent(state = state, onEvent = viewModel::onEvent, contentPadding = contentPadding, modifier = modifier)
}

@Composable
fun CatsListContent(
    state: CatsListState,
    onEvent: (CatsListEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (val status = state.status) {
            CatsListUiStatus.Content -> CatsFeed(cats = state.cats, onEvent = onEvent, contentPadding = contentPadding)
            CatsListUiStatus.Loading -> LoadingIndicator()
            is CatsListUiStatus.Error -> ErrorMessage(
                message = status.message,
                onRetry = { onEvent(CatsListEvent.Retry) },
            )
        }
    }
}

@Composable
private fun CatsFeed(cats: ImmutableList<Cat>, onEvent: (CatsListEvent) -> Unit, contentPadding: PaddingValues) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember(cats) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            cats.isNotEmpty() && lastVisibleIndex >= cats.lastIndex
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onEvent(CatsListEvent.LoadMore)
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(items = cats, key = { it.id }) { cat ->
            CatItem(
                cat = cat,
                onFavoriteClick = { onEvent(CatsListEvent.ToggleFavorite(cat)) },
                onDownloadClick = { onEvent(CatsListEvent.Download(cat)) },
            )
        }
    }
}

@Preview(name = "Content", showBackground = true)
@Composable
private fun CatsListContentPreview() {
    CatsListTheme {
        CatsListContent(
            state = CatsListState(
                status = CatsListUiStatus.Content,
                cats = persistentListOf(
                    Cat(id = "1", url = "", width = 300, height = 300, isFavorite = false),
                    Cat(id = "2", url = "", width = 300, height = 300, isFavorite = true),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun CatsListErrorPreview() {
    CatsListTheme {
        CatsListContent(
            state = CatsListState(
                status = CatsListUiStatus.Error(UiText.Resource(R.string.catslist_error_loading_cats)),
            ),
            onEvent = {},
        )
    }
}
