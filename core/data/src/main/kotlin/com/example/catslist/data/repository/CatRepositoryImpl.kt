package com.example.catslist.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.CatFeedDao
import com.example.catslist.data.local.toDomain
import com.example.catslist.data.local.toEntity
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
@Singleton
class CatRepositoryImpl @Inject constructor(
    private val catDao: CatDao,
    private val catFeedDao: CatFeedDao,
    private val catFeedRemoteMediator: CatFeedRemoteMediator,
) : CatRepository {

    override val favorites: Flow<ImmutableList<Cat>> =
        catDao.getAllCats().map { entities -> entities.map { it.toDomain() }.toPersistentList() }

    /**
     * No favorite status here, ever — not baked in via a query join (see
     * `CatFeedDao.pagingSource`'s doc) and not layered on via `combine()` either.
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
            // Defaults to pageSize * 3. The mediator fetches exactly one page per network call,
            // so a larger initial load leaves Paging short and it immediately appends to catch
            // up — and every append writes feedCatsTable, invalidating the very PagingSource
            // reading it. That produced a visible cascade at startup: cats, then a new
            // generation reloading, then cats again, three times before the screen settled.
            initialLoadSize = PAGE_SIZE,
            enablePlaceholders = false,
        ),
        remoteMediator = catFeedRemoteMediator,
        pagingSourceFactory = catFeedDao::pagingSource,
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun toggleFavorite(cat: Cat) = catDao.toggleFavorite(cat.toEntity())

    override suspend fun removeFavorite(cat: Cat) {
        catDao.deleteCat(cat.toEntity())
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
