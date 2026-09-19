package com.example.catslist.presentation.catslist

import com.example.catslist.domain.model.Cat

internal sealed interface CatsListEvent {
    data class ToggleFavorite(
        val cat: Cat,
    ) : CatsListEvent
    data class Download(
        val cat: Cat,
    ) : CatsListEvent
}
