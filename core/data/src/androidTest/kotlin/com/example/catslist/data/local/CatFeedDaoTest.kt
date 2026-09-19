package com.example.catslist.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against a real in-memory Room database because the behaviour under test — Room
 * actually generating and executing [CatFeedDao.pagingSource]'s `@Query` — is Room's own,
 * not something [com.example.catslist.testing.FakeCatFeedDao] reproduces.
 */
@RunWith(AndroidJUnit4::class)
class CatFeedDaoTest {

    private lateinit var database: CatDatabase
    private lateinit var catDao: CatDao
    private lateinit var feedDao: CatFeedDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CatDatabase::class.java).build()
        catDao = database.catDao()
        feedDao = database.catFeedDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun pagingSource_ordersByFetchOrder() = runBlocking {
        feedDao.refresh(listOf(feedEntity("2", sortOrder = 1), feedEntity("1", sortOrder = 0)), nextPage = 1)

        val snapshot = pager().flow.asSnapshot()

        assertEquals(listOf("1", "2"), snapshot.map { it.id })
    }

    /**
     * The regression this guards: [pagingSource] used to join against `favoriteCatsTable`
     * so the feed's favorite icon could update live. That made Room's invalidation tracker
     * treat every favorite toggle as a reason to hand Paging a new generation, and a new
     * generation makes Paging re-run the `RemoteMediator`'s REFRESH — wiping the entire
     * cached feed back to page 0 on every single toggle. `isFavorite` is applied later, in
     * `CatRepositoryImpl.feed`, specifically so a write to `favoriteCatsTable` never reaches
     * this query at all.
     */
    @Test
    fun pagingSource_isNotInvalidatedByAFavoriteToggle() = runBlocking {
        feedDao.refresh(listOf(feedEntity("1", sortOrder = 0)), nextPage = 1)
        feedDao.append(listOf(feedEntity("2", sortOrder = 1)), nextPage = 2)

        catDao.toggleFavorite(catEntity("1"))

        val snapshot = pager().flow.asSnapshot()
        assertEquals("a favorite toggle must not wipe the cached feed", listOf("1", "2"), snapshot.map { it.id })
    }

    @Test
    fun append_addsAfterWhatRefreshAlreadyCached() = runBlocking {
        feedDao.refresh(listOf(feedEntity("1", sortOrder = 0)), nextPage = 1)

        feedDao.append(listOf(feedEntity("2", sortOrder = feedDao.nextSortOrder())), nextPage = 2)

        val snapshot = pager().flow.asSnapshot()
        assertEquals(listOf("1", "2"), snapshot.map { it.id })
        assertEquals(FeedRemoteKeysEntity(nextPage = 2), feedDao.getRemoteKeys())
    }

    @Test
    fun refresh_replacesWhatWasCachedBefore() = runBlocking {
        feedDao.refresh(listOf(feedEntity("stale")), nextPage = 1)

        feedDao.refresh(listOf(feedEntity("fresh")), nextPage = 1)

        val snapshot = pager().flow.asSnapshot()
        assertEquals(listOf("fresh"), snapshot.map { it.id })
    }

    private fun pager() = Pager(PagingConfig(pageSize = 10)) { feedDao.pagingSource() }

    private fun feedEntity(id: String, sortOrder: Int = 0) = FeedCatEntity(
        id = id,
        url = "https://cdn.example/$id.jpg",
        width = 300,
        height = 200,
        sortOrder = sortOrder,
    )

    private fun catEntity(id: String) = CatEntity(
        id = id,
        url = "https://cdn.example/$id.jpg",
        width = 300,
        height = 200,
    )
}
