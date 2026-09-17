package com.example.catslist.presentation.catslist

import androidx.compose.runtime.Immutable
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface CatsListUiStatus {
    // Declared most-likely first, and every `when` over this mirrors the order — so the
    // branch order is checkable against this list instead of being an unverifiable claim.
    data object Content : CatsListUiStatus
    data object Loading : CatsListUiStatus

    /** No `retryable` flag: [CatsListEvent.Retry] recovers from every failure the screen has. */
    data class Error(
        val message: UiText,
    ) : CatsListUiStatus
}

@Immutable
internal data class CatsListState(
    val status: CatsListUiStatus = CatsListUiStatus.Loading,
    val cats: ImmutableList<Cat> = persistentListOf(),
)

internal sealed interface CatsListEvent {
    /** Reaching the end of the list. Appends a page; does nothing about a broken feed. */
    data object LoadMore : CatsListEvent

    /** The user asking to recover from an error: resubscribes to the feed and loads a page. */
    data object Retry : CatsListEvent

    data class ToggleFavorite(
        val cat: Cat,
    ) : CatsListEvent
    data class Download(
        val cat: Cat,
    ) : CatsListEvent
}
