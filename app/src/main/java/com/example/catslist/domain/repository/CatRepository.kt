package com.example.catslist.domain.repository

import com.example.catslist.domain.model.Cat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for cats: the in-memory "infinite scroll" feed and
 * the Room-backed favorites, both as [Flow]s so every observer stays in sync
 * without a hand-rolled listener bus.
 */
interface CatRepository {

    /** Cats fetched so far this session, each reflecting current favorite status. */
    val feed: Flow<ImmutableList<Cat>>

    /** Cats persisted as favorites. */
    val favorites: Flow<ImmutableList<Cat>>

    /** Fetches the next batch of not-yet-seen cats from the API and appends them to [feed]. */
    suspend fun fetchNextBatch()

    /** Adds [cat] to favorites if absent, removes it otherwise. */
    suspend fun toggleFavorite(cat: Cat)

    /** Removes [cat] from favorites. */
    suspend fun removeFavorite(cat: Cat)
}
