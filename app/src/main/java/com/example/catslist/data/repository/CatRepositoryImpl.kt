package com.example.catslist.data.repository

import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.toDomain
import com.example.catslist.data.local.toEntity
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.toDomain
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatRepositoryImpl @Inject constructor(
    private val catApiService: CatApiService,
    private val catDao: CatDao,
) : CatRepository {

    /** Cats fetched this session, before favorite status is layered on. Not persisted. */
    private val fetched = MutableStateFlow<List<Cat>>(emptyList())

    override val favorites: Flow<List<Cat>> =
        catDao.getAllCats().map { entities -> entities.map { it.toDomain() } }

    override val feed: Flow<List<Cat>> =
        combine(fetched, favorites) { fetchedCats, favoriteCats ->
            val favoriteIds = favoriteCats.mapTo(hashSetOf()) { it.id }
            fetchedCats.map { it.copy(isFavorite = it.id in favoriteIds) }
        }

    override suspend fun fetchNextBatch() {
        val existingIds = fetched.value.mapTo(hashSetOf()) { it.id }
        val newCats = catApiService.requestCatInfo(limit = PAGE_SIZE)
            .map { it.toDomain() }
            .distinctBy { it.id }
            .filterNot { it.id in existingIds }
        fetched.value = fetched.value + newCats
    }

    override suspend fun toggleFavorite(cat: Cat) = catDao.toggleFavorite(cat.toEntity())

    override suspend fun removeFavorite(cat: Cat) {
        catDao.deleteCat(cat.toEntity())
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
