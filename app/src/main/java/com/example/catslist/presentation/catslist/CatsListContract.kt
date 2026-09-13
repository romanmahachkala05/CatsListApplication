package com.example.catslist.presentation.catslist

import androidx.compose.runtime.Immutable
import com.example.catslist.presentation.UiText
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface CatsListUiStatus {
    data object Loading : CatsListUiStatus
    data object Content : CatsListUiStatus
    data class Error(val message: UiText, val retryable: Boolean) : CatsListUiStatus
}

@Immutable
data class CatsListState(
    val status: CatsListUiStatus = CatsListUiStatus.Loading,
    val cats: ImmutableList<Cat> = persistentListOf(),
)

sealed interface CatsListEvent {
    data object LoadMore : CatsListEvent
    data class ToggleFavorite(val cat: Cat) : CatsListEvent
    data class Download(val cat: Cat) : CatsListEvent
}
