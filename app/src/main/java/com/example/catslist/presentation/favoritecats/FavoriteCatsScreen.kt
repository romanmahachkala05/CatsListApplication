package com.example.catslist.presentation.favoritecats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.catslist.R
import com.example.catslist.presentation.TextSource
import com.example.catslist.presentation.components.CatItem
import com.example.catslist.presentation.components.EmptyMessage
import com.example.catslist.presentation.components.LoadingIndicator
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FavoriteCatsScreen(modifier: Modifier = Modifier, viewModel: FavoriteCatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FavoriteCatsContent(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun FavoriteCatsContent(
    state: FavoriteCatsState,
    onEvent: (FavoriteCatsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state.status) {
            FavoriteCatsUiStatus.Loading -> LoadingIndicator()
            FavoriteCatsUiStatus.Empty -> EmptyMessage(TextSource.Res(R.string.empty_favorites_message))
            FavoriteCatsUiStatus.Content -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
