package com.example.catslist.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.data.local.CatDatabase
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.CatDto
import com.example.catslist.domain.model.Cat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the full `Pager` + `RemoteMediator` + Room stack `CatRepositoryImpl.feed` actually
 * builds, not a fake standing in for it — proving the RemoteMediator/`Pager` combination
 * behaves under a real dispatcher isn't something a plain JVM `runTest` reliably does.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class CatRepositoryImplTest {

    private lateinit var database: CatDatabase
    private lateinit var repository: CatRepositoryImpl
    private lateinit var api: FakeApi

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CatDatabase::class.java).build()
        api = FakeApi()
        val mediator = CatFeedRemoteMediator(api, database.catFeedDao())
        repository = CatRepositoryImpl(database.catDao(), database.catFeedDao(), mediator)
    }

    @After
    fun tearDown() = database.close()

    /**
     * `feed` never carries favorite status — see `CatRepositoryImpl.feed`'s own doc for why
     * (ADR-0023): the screen applies it at render time instead. A `false` here regardless of
     * what is favorited is the contract, not an oversight.
     */
    @Test
    fun feedNeverCarriesFavoriteStatus() = runBlocking {
        api.enqueue(dto("1"))
        repository.toggleFavorite(cat("1"))

        val snapshot = repository.feed.asSnapshot()

        assertEquals(false, snapshot.single().isFavorite)
    }

    /**
     * The regression this guards against (see ADR-0023): both a favoriting-invalidates-the-feed
     * query design, and later a `combine()` re-mapping the same `PagingData` on every favorite
     * change, made this fail — the first by wiping every page already loaded back to page 0,
     * the second by crashing with "Attempt to collect twice from pageEventFlow".
     */
    @Test
    fun favoritingDoesNotResetAlreadyLoadedPages() = runBlocking {
        api.enqueue(dto("1"))
        api.enqueue(dto("2"))

        // Both calls inside one collection: `feed` is a single Pager instance, and asSnapshot
        // starting a second, independent collection against it is not what it is meant for.
        val snapshot = repository.feed.asSnapshot {
            appendScrollWhile { it.id != "2" }
            repository.toggleFavorite(cat("1"))
        }

        assertEquals(
            "a favorite toggle must not wipe cats already loaded",
            listOf("1", "2"),
            snapshot.map { it.id },
        )
    }

    private fun dto(id: String) = CatDto(id = id, url = "https://cdn.example/$id.jpg", width = 300, height = 200)

    private fun cat(id: String) =
        Cat(id = id, url = "https://cdn.example/$id.jpg", width = 300, height = 200, isFavorite = false)

    private class FakeApi : CatApiService {
        private val responses = ArrayDeque<List<CatDto>>()
        fun enqueue(vararg cats: CatDto) = responses.addLast(cats.toList())
        override suspend fun requestCatInfo(limit: Int, page: Int): List<CatDto> =
            responses.removeFirstOrNull().orEmpty()
    }
}
