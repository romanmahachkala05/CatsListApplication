package com.example.catslist.presentation.favoritecats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catslist.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.EmptyMessage
import com.example.catslist.presentation.components.ErrorMessage
import com.example.catslist.presentation.components.LoadingIndicator
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.persistentListOf

/** The download/favorite Snackbar for [viewModel] is collected by the app's shared
 * host (see MainActivity) so it survives a tab switch — not collected here. */
@Composable
fun FavoriteCatsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: FavoriteCatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoriteCatsContent(state = state, onEvent = viewModel::onEvent, contentPadding = contentPadding, modifier = modifier)
}

@Composable
fun FavoriteCatsContent(
    state: FavoriteCatsState,
    onEvent: (FavoriteCatsEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (val status = state.status) {
            FavoriteCatsUiStatus.Loading -> LoadingIndicator()
            is FavoriteCatsUiStatus.Error -> ErrorMessage(message = status.message, onRetry = null)
            FavoriteCatsUiStatus.Empty -> EmptyMessage(UiText.Resource(R.string.favoritecats_empty_message))
            FavoriteCatsUiStatus.Content -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                items(items = state.cats, key = { it.id }) { cat ->
                    CatItem(
                        cat = cat,
                        onFavoriteClick = { onEvent(FavoriteCatsEvent.RemoveFavorite(cat)) },
                        onDownloadClick = { onEvent(FavoriteCatsEvent.Download(cat)) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Content", showBackground = true)
@Composable
private fun FavoriteCatsContentPreview() {
    CatsListTheme {
        FavoriteCatsContent(
            state = FavoriteCatsState(
                status = FavoriteCatsUiStatus.Content,
                cats = persistentListOf(
                    Cat(id = "1", url = "", width = 300, height = 300, isFavorite = true),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun FavoriteCatsEmptyPreview() {
    CatsListTheme {
        FavoriteCatsContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Empty),
            onEvent = {},
        )
    }
}
