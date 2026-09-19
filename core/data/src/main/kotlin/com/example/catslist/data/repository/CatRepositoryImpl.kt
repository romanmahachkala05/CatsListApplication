package com.example.catslist.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.toDomain
import com.example.catslist.data.local.toEntity
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.CatFeedPagingSource
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CatRepositoryImpl @Inject constructor(
    private val catDao: CatDao,
    private val catApiService: CatApiService,
) : CatRepository {

    override val favorites: Flow<ImmutableList<Cat>> =
        catDao.getAllCats().map { entities -> entities.map { it.toDomain() }.toPersistentList() }

    /**
     * No favorite status here, ever — and not layered on via `combine()` either.
     * `PagingData.map` is not safe to re-run on the same underlying `PagingData` more than
     * once: `combine()`-ing this with a changing favorites flow re-invoked `.map` on the same
     * instance on every favorite toggle, and the second, still-live subscription to the same
     * generation's internal event stream crashed with "Attempt to collect twice from
     * pageEventFlow". The live overlay belongs in the UI layer instead — `CatsListScreen`
     * combines [ImmutableList]<Cat> from [favorites] with the plain `LazyPagingItems` from
     * this at render time, which is ordinary Compose recomposition, not a second Paging
     * generation.
     */
    override val feed: Flow<PagingData<Cat>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            // Must equal pageSize. The API pages by (page, limit), so page N holds items
            // N * limit onwards — a first load of a different size would put every later page
            // at the wrong offset and quietly skip or repeat a block of cats.
            initialLoadSize = PAGE_SIZE,
            // Defaults to pageSize, which at this card size is far more lookahead than the
            // screen needs: about two cards are visible, so a ten-item distance is already
            // satisfied the moment the first page lands.
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        // A new source per generation, never a shared instance: a PagingSource is single-use
        // once invalidated, and each one owns the de-duplication state for its own generation.
        pagingSourceFactory = { CatFeedPagingSource(catApiService) },
    ).flow

    override suspend fun toggleFavorite(cat: Cat) = catDao.toggleFavorite(cat.toEntity())

    override suspend fun removeFavorite(cat: Cat) {
        catDao.deleteCat(cat.toEntity())
    }

    private companion object {
        const val PAGE_SIZE = 10

        /** Roughly one screen of cards ahead of the last visible one. */
        const val PREFETCH_DISTANCE = 3
    }
}
