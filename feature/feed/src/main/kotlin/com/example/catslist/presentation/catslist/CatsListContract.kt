package com.example.catslist.presentation.catslist

import androidx.compose.runtime.Immutable
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * Whether the favorite overlay is still live. Deliberately *not* a screen-wide `UiStatus` like
 * every other screen has: the paged list's own loading, error and retry are Paging's, read from
 * `LazyPagingItems.loadState` in the Composable (ADR-0023). This status covers only the part of
 * the screen Paging never loads.
 */
@Immutable
internal sealed interface CatsListFavoritesStatus {
    // Declared most-likely first, and every `when` over this mirrors the order.
    data object Live : CatsListFavoritesStatus

    /** The stream ended in a failure, so [CatsListState.favoriteIds] will never change again. */
    data object Unavailable : CatsListFavoritesStatus
}

@Immutable
internal data class CatsListState(
    val favoritesStatus: CatsListFavoritesStatus = CatsListFavoritesStatus.Live,
    /**
     * Ids of the favorited cats, overlaid onto the paged cats at render time. A paged cat never
     * carries its own favorite status — attaching it in SQL made a toggle invalidate the paging
     * query and wipe the cached feed; see ADR-0023.
     */
    val favoriteIds: ImmutableSet<String> = persistentSetOf(),
)

internal sealed interface CatsListEvent {
    data class ToggleFavorite(
        val cat: Cat,
    ) : CatsListEvent
    data class Download(
        val cat: Cat,
    ) : CatsListEvent
}
