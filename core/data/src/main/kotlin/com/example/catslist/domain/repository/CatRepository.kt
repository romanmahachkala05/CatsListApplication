package com.example.catslist.domain.repository

import androidx.paging.PagingData
import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for cats: the paged, Room-cached feed and the Room-backed
 * favorites, both as [Flow]s so every observer stays in sync without a hand-rolled
 * listener bus.
 */
interface CatRepository {

    /**
     * The feed, one page at a time. Paging owns when the next page loads (driven by
     * scroll position) and caches loaded pages into Room, so a cat's [Cat.isFavorite]
     * stays live — toggling it invalidates the underlying query the same way
     * [favorites] already does.
     */
    val feed: Flow<PagingData<Cat>>

    /** Cats persisted as favorites. */
    val favorites: Flow<ImmutableList<Cat>>

    /** Adds [cat] to favorites if absent, removes it otherwise. */
    suspend fun toggleFavorite(cat: Cat)

    /** Removes [cat] from favorites. */
    suspend fun removeFavorite(cat: Cat)
}
