package com.example.catslist.presentation.favoritecats

import androidx.compose.runtime.Immutable
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface FavoriteCatsUiStatus {
    // Declared most-likely first, and every `when` over this mirrors the order — so the
    // branch order is checkable against this list instead of being an unverifiable claim.
    data object Content : FavoriteCatsUiStatus
    data object Empty : FavoriteCatsUiStatus
    data object Loading : FavoriteCatsUiStatus

    /** No `retryable` flag: [FavoriteCatsEvent.Retry] recovers from the one failure there is. */
    data class Error(
        val message: UiText,
    ) : FavoriteCatsUiStatus
}

@Immutable
data class FavoriteCatsState(
    val status: FavoriteCatsUiStatus = FavoriteCatsUiStatus.Loading,
    val cats: ImmutableList<Cat> = persistentListOf(),
)

sealed interface FavoriteCatsEvent {
    /** The user asking to recover from an error: resubscribes to the favorites stream. */
    data object Retry : FavoriteCatsEvent

    data class RemoveFavorite(
        val cat: Cat,
    ) : FavoriteCatsEvent
    data class Download(
        val cat: Cat,
    ) : FavoriteCatsEvent
}
