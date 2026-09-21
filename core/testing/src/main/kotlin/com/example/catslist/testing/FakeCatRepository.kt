package com.example.catslist.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * In-memory [CatRepository]. As in the real one, [feed] never carries favorite status
 * (ADR-0024): a consumer overlays it from [favorites].
 *
 * [feed] is one static, already-loaded page, which is enough for event handling. Real paging
 * belongs to `CatRepositoryImplTest`, which exercises the genuine `Pager`.
 */
class FakeCatRepository : CatRepository {

    private val fetched = MutableStateFlow<List<Cat>>(emptyList())
    private val favorited = MutableStateFlow<List<Cat>>(emptyList())

    /** When set, [favorites] fails with it on every emission. */
    var favoritesError: Throwable? = null

    /** When set, [toggleFavorite] and [removeFavorite] fail with it instead of writing. */
    var favoriteError: Throwable? = null

    override val favorites: Flow<ImmutableList<Cat>> =
        favorited.asStateFlow()
            .map { it.toPersistentList() }
            .onEach { favoritesError?.let { error -> throw error } }

    override val feed: Flow<PagingData<Cat>> = fetched.map { cats ->
        // Fully loaded in every direction: `asSnapshot()` otherwise waits for an append
        // that will never resolve.
        PagingData.from(
            data = cats,
            sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            ),
        )
    }

    /** Replaces the feed outright; there is no page-by-page fetch to simulate. */
    fun setFeed(vararg cats: Cat) {
        fetched.value = cats.map { it.copy(isFavorite = false) }
    }

    fun setFavorites(vararg cats: Cat) {
        favorited.value = cats.map { it.copy(isFavorite = true) }
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
