package com.example.catslist.data.repository

import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.toDomain
import com.example.catslist.data.local.toEntity
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.toDomain
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class CatRepositoryImpl @Inject constructor(
    private val catApiService: CatApiService,
    private val catDao: CatDao,
) : CatRepository {

    /** Cats fetched this session, before favorite status is layered on. Not persisted. */
    private val fetched = MutableStateFlow<List<Cat>>(emptyList())

    override val favorites: Flow<ImmutableList<Cat>> =
        catDao.getAllCats().map { entities -> entities.map { it.toDomain() }.toPersistentList() }

    override val feed: Flow<ImmutableList<Cat>> =
        combine(fetched, favorites) { fetchedCats, favoriteCats ->
            val favoriteIds = favoriteCats.mapTo(hashSetOf()) { it.id }
            // Only cats whose favorite status actually changed get a new instance — favoriting
            // one cat should not reallocate every other cat fetched this session.
            fetchedCats.map { cat ->
                val isFavorite = cat.id in favoriteIds
                if (cat.isFavorite == isFavorite) cat else cat.copy(isFavorite = isFavorite)
            }.toPersistentList()
        }

    override suspend fun fetchNextBatch() {
        val newCats = catApiService.requestCatInfo(limit = PAGE_SIZE)
            .map { it.toDomain() }
            .distinctBy { it.id }

        // Requests may overlap; this update may not. `update` is a compare-and-set loop, so
        // the ids are read from the same `current` that gets written — two concurrent loads
        // cannot each filter against a snapshot the other has already added to, and neither
        // can lose the other's write. `fetched.value = fetched.value + …` would read and
        // write separately: safe only for as long as every caller happens to resume on the
        // same thread, which is not something this function can promise.
        //
        // A duplicate id would reach `items(cats, key = { it.id })` and crash the LazyColumn,
        // and a lost write would silently drop a page.
        //
        // The lambda re-runs on contention, so it stays free of side effects.
        fetched.update { current ->
            val existingIds = current.mapTo(hashSetOf()) { it.id }
            current + newCats.filterNot { it.id in existingIds }
        }
    }

    override suspend fun toggleFavorite(cat: Cat) = catDao.toggleFavorite(cat.toEntity())

    override suspend fun removeFavorite(cat: Cat) {
        catDao.deleteCat(cat.toEntity())
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
