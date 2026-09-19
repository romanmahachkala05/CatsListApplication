package com.example.catslist.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.catslist.data.local.CatFeedDao
import com.example.catslist.data.local.FeedCatEntity
import com.example.catslist.data.local.toFeedEntity
import com.example.catslist.data.remote.CatApiService
import java.io.IOException
import javax.inject.Inject
import retrofit2.HttpException

/**
 * Feeds [CatFeedDao.pagingSource] from the network. Only appends: TheCatAPI's search
 * endpoint has no signal for "cats newer than what I have", so [LoadType.PREPEND] never
 * has anything to fetch and [LoadType.REFRESH] always restarts from page 0.
 */
@OptIn(ExperimentalPagingApi::class)
class CatFeedRemoteMediator @Inject constructor(
    private val catApiService: CatApiService,
    private val catFeedDao: CatFeedDao,
) : RemoteMediator<Int, FeedCatEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, FeedCatEntity>): MediatorResult {
        val page = resolvePage(loadType) ?: return MediatorResult.Success(endOfPaginationReached = true)

        return try {
            val cats = catApiService.requestCatInfo(limit = state.config.pageSize, page = page).distinctBy { it.id }
            val nextPage = if (cats.isEmpty()) null else page + 1

            if (loadType == LoadType.REFRESH) {
                val entities = cats.mapIndexed { index, dto -> dto.toFeedEntity(sortOrder = index) }
                catFeedDao.refresh(entities, nextPage)
            } else {
                val startOrder = catFeedDao.nextSortOrder()
                val entities = cats.mapIndexed { index, dto -> dto.toFeedEntity(sortOrder = startOrder + index) }
                catFeedDao.append(entities, nextPage)
            }

            MediatorResult.Success(endOfPaginationReached = cats.isEmpty())
        } catch (error: IOException) {
            MediatorResult.Error(error)
        } catch (error: HttpException) {
            MediatorResult.Error(error)
        }
    }

    /** The page to fetch, or `null` when this [loadType] has nothing left to load. */
    private suspend fun resolvePage(loadType: LoadType): Int? = when (loadType) {
        LoadType.REFRESH -> STARTING_PAGE
        LoadType.PREPEND -> null
        LoadType.APPEND -> catFeedDao.getRemoteKeys()?.nextPage
    }

    private companion object {
        const val STARTING_PAGE = 0
    }
}
