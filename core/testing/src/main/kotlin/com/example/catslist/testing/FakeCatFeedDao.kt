package com.example.catslist.testing

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.catslist.data.local.CatFeedDao
import com.example.catslist.data.local.FeedCatEntity
import com.example.catslist.data.local.FeedRemoteKeysEntity

/**
 * In-memory stand-in for [CatFeedDao]. Extends the real DAO rather than reimplementing it,
 * so [refresh]/[append] under test are the very transaction bodies Room runs — see
 * [FakeCatDao] for the same reasoning. [pagingSource] is a plain offset-paged, single-
 * generation `PagingSource` — good enough to prove `CatRepositoryImpl.feed`'s `combine()`
 * overlay, not a stand-in for Room's own invalidation behaviour (that's
 * [com.example.catslist.data.local.CatFeedDaoTest], on a real database).
 */
class FakeCatFeedDao : CatFeedDao() {

    private val cats = mutableListOf<FeedCatEntity>()
    private var remoteKeys: FeedRemoteKeysEntity? = null

    override fun pagingSource(): PagingSource<Int, FeedCatEntity> = object : PagingSource<Int, FeedCatEntity>() {
        override fun getRefreshKey(state: PagingState<Int, FeedCatEntity>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedCatEntity> {
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, cats.size)
            val page = if (start < end) cats.subList(start, end) else emptyList()
            return LoadResult.Page(
                data = page,
                prevKey = if (start == 0) null else maxOf(0, start - params.loadSize),
                nextKey = if (end >= cats.size) null else end,
            )
        }
    }

    override suspend fun insertAll(cats: List<FeedCatEntity>) {
        val incomingIds = cats.mapTo(hashSetOf()) { it.id }
        this.cats.removeAll { it.id in incomingIds }
        this.cats += cats
    }

    override suspend fun clearFeed() {
        cats.clear()
    }

    override suspend fun nextSortOrder(): Int = (cats.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getRemoteKeys(): FeedRemoteKeysEntity? = remoteKeys

    override suspend fun setRemoteKeys(keys: FeedRemoteKeysEntity) {
        remoteKeys = keys
    }

    fun cachedCats(): List<FeedCatEntity> = cats.toList()
}
