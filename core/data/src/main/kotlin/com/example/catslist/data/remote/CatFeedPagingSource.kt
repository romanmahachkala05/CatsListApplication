package com.example.catslist.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.catslist.domain.model.Cat
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import retrofit2.HttpException

/**
 * Pages the feed straight from TheCatAPI, keeping nothing on disk.
 *
 * Only favorites are stored locally. The feed is whatever the network says it is right now, so a
 * launch shows cats fetched on that launch rather than the previous run's, and there is no cached
 * copy to render first and replace a moment later.
 */
class CatFeedPagingSource(
    private val catApiService: CatApiService,
) : PagingSource<Int, Cat>() {

    /**
     * Ids this generation has already handed out.
     *
     * TheCatAPI's search endpoint repeats cats, both inside one response and across pages, and
     * the list keys its items by id — a repeat is a crash, not a cosmetic double (ADR-0015).
     * The Room-backed cache this replaced got that for free from a primary key; without it the
     * source has to remember. A set per generation is the right scope: a refresh builds a new
     * source, and its cats are allowed to be the same ones as before.
     */
    private val seenIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Always restarts at the first page. The feed is an endless stream of random cats with no
     * stable position to return to, so resuming mid-list after invalidation would mean skipping
     * whatever the API would now serve earlier.
     */
    override fun getRefreshKey(state: PagingState<Int, Cat>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cat> {
        val page = params.key ?: STARTING_PAGE
        return try {
            val cats = catApiService.requestCatInfo(limit = params.loadSize, page = page)
            LoadResult.Page(
                data = cats.filter { seenIds.add(it.id) }.map { it.toDomain() },
                // The feed only ever appends: the API has no notion of cats newer than the ones
                // already held, so there is never anything above the first page to fetch.
                prevKey = null,
                // Keyed off the response, not the de-duplicated list: a page that happened to be
                // all repeats still means there are more pages behind it.
                nextKey = if (cats.isEmpty()) null else page + 1,
            )
        } catch (error: IOException) {
            LoadResult.Error(error)
        } catch (error: HttpException) {
            LoadResult.Error(error)
        }
    }

    private companion object {
        const val STARTING_PAGE = 0
    }
}
