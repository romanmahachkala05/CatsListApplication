package com.example.catslist.testing

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

/**
 * In-memory [CatRepository] with the same observable behaviour as the real one:
 * both flows stay live, and favoriting a cat is reflected in [feed] without
 * re-fetching. Batches are queued up front because a ViewModel loads its first
 * page from `init`, i.e. before the test gets a reference back.
 */
class FakeCatRepository : CatRepository {

    private val fetched = MutableStateFlow<List<Cat>>(emptyList())
    private val favorited = MutableStateFlow<List<Cat>>(emptyList())
    private val batches = ArrayDeque<List<Cat>>()

    /** When set, every [fetchNextBatch] fails with it instead of returning cats. */
    var fetchError: Throwable? = null

    /** When set, [toggleFavorite] and [removeFavorite] fail with it instead of writing. */
    var favoriteError: Throwable? = null

    /** When set, [feed] fails with it on every emission, as a broken Room query would. */
    var feedError: Throwable? = null

    /** When set, [favorites] fails with it on every emission. */
    var favoritesError: Throwable? = null

    var fetchCount: Int = 0
        private set

    override val favorites: Flow<List<Cat>> =
        favorited.asStateFlow().onEach { favoritesError?.let { error -> throw error } }

    override val feed: Flow<List<Cat>> = combine(fetched, favorited) { cats, favorites ->
        val favoriteIds = favorites.mapTo(hashSetOf()) { it.id }
        cats.map { it.copy(isFavorite = it.id in favoriteIds) }
    }.onEach { feedError?.let { error -> throw error } }

    /** Queues one batch per [fetchNextBatch] call, in order. Exhausted queue means empty batches. */
    fun enqueueBatch(vararg cats: Cat) = batches.addLast(cats.toList())

    fun setFavorites(vararg cats: Cat) {
        favorited.value = cats.map { it.copy(isFavorite = true) }
    }

    override suspend fun fetchNextBatch() {
        fetchCount++
        fetchError?.let { throw it }
        val batch = batches.removeFirstOrNull().orEmpty()
        val known = fetched.value.mapTo(hashSetOf()) { it.id }
        fetched.value = fetched.value + batch.filterNot { it.id in known }
    }

    override suspend fun toggleFavorite(cat: Cat) {
        favoriteError?.let { throw it }
        if (favorited.value.any { it.id == cat.id }) removeFavorite(cat) else addFavorite(cat)
    }

    override suspend fun removeFavorite(cat: Cat) {
        favoriteError?.let { throw it }
        favorited.value = favorited.value.filterNot { it.id == cat.id }
    }

    private fun addFavorite(cat: Cat) {
        favorited.value = favorited.value + cat.copy(isFavorite = true)
    }
}
