package com.example.catslist.presentation.favoritecats

import androidx.compose.runtime.Immutable
import com.example.catslist.presentation.TextSource
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface FavoriteCatsUiStatus {
    data object Loading : FavoriteCatsUiStatus
    data object Content : FavoriteCatsUiStatus
    data object Empty : FavoriteCatsUiStatus
}

@Immutable
data class FavoriteCatsState(
    val status: FavoriteCatsUiStatus = FavoriteCatsUiStatus.Loading,
    val cats: ImmutableList<Cat> = persistentListOf(),
)

sealed interface FavoriteCatsEvent {
    data class RemoveFavorite(val cat: Cat) : FavoriteCatsEvent
    data class Download(val cat: Cat) : FavoriteCatsEvent
}

/** One-shot signals the Screen consumes once (e.g. a Snackbar) — never part of [FavoriteCatsState]. */
sealed interface FavoriteCatsEffect {
    data class ShowMessage(val message: TextSource) : FavoriteCatsEffect
}
