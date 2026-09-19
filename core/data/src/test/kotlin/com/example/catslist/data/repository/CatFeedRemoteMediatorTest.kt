package com.example.catslist.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.catslist.data.local.FeedCatEntity
import com.example.catslist.data.local.FeedRemoteKeysEntity
import com.example.catslist.testing.FakeCatApiService
import com.example.catslist.testing.FakeCatFeedDao
import com.example.catslist.testing.catDto
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class CatFeedRemoteMediatorTest {

    private val api = FakeCatApiService()
    private val dao = FakeCatFeedDao()
    private val mediator = CatFeedRemoteMediator(api, dao)

    @Test
    fun `an empty cache is refreshed on launch`() = runTest {
        assertThat(mediator.initialize()).isEqualTo(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH)
    }

    @Test
    fun `a cached feed is opened as it is, without a network refresh`() = runTest {
        // Paging's default would wipe this and refetch page 0 on every cold start: a second
        // load right after the first, and an empty screen when there is no network. Cached
        // cats are only previously seen, not wrong — pulling to refresh asks for new ones.
        api.enqueueResponse(catDto("1"))
        mediator.load(LoadType.REFRESH, state())

        assertThat(mediator.initialize()).isEqualTo(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH)
    }

    @Test
    fun `refresh replaces the cache and requests page 0`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"))

        val result = mediator.load(LoadType.REFRESH, state())

        assertThat(result.endOfPaginationReached()).isFalse()
        assertThat(dao.cachedCats().map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(api.lastRequestedPage).isEqualTo(0)
    }

    @Test
    fun `refresh drops duplicates within one page`() = runTest {
        // TheCatAPI's search endpoint can hand back the same cat twice in one response.
        api.enqueueResponse(catDto("1"), catDto("1"), catDto("2"))

        mediator.load(LoadType.REFRESH, state())

        assertThat(dao.cachedCats().map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `refresh discards whatever was cached before it`() = runTest {
        api.enqueueResponse(catDto("stale"))
        mediator.load(LoadType.REFRESH, state())
        api.enqueueResponse(catDto("fresh"))

        mediator.load(LoadType.REFRESH, state())

        assertThat(dao.cachedCats().map { it.id }).containsExactly("fresh")
    }

    @Test
    fun `an empty refresh response ends pagination`() = runTest {
        val result = mediator.load(LoadType.REFRESH, state())

        assertThat(result.endOfPaginationReached()).isTrue()
    }

    @Test
    fun `append fetches the page stored by the previous load and keeps what was already cached`() = runTest {
        api.enqueueResponse(catDto("1"))
        mediator.load(LoadType.REFRESH, state())
        api.enqueueResponse(catDto("2"))

        val result = mediator.load(LoadType.APPEND, state())

        assertThat(result.endOfPaginationReached()).isFalse()
        assertThat(dao.cachedCats().map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(api.lastRequestedPage).isEqualTo(1)
    }

    @Test
    fun `append with no stored key ends pagination without a network call`() = runTest {
        val result = mediator.load(LoadType.APPEND, state())

        assertThat(result.endOfPaginationReached()).isTrue()
        assertThat(api.lastRequestedPage).isNull()
    }

    @Test
    fun `an empty append response ends pagination and clears the next key`() = runTest {
        api.enqueueResponse(catDto("1"))
        mediator.load(LoadType.REFRESH, state())
        api.enqueueResponse()

        mediator.load(LoadType.APPEND, state())

        assertThat(dao.getRemoteKeys()).isEqualTo(FeedRemoteKeysEntity(nextPage = null))
    }

    @Test
    fun `prepend is always a no-op`() = runTest {
        val result = mediator.load(LoadType.PREPEND, state())

        assertThat(result.endOfPaginationReached()).isTrue()
        assertThat(api.lastRequestedPage).isNull()
    }

    @Test
    fun `a network failure becomes a mediator error, not an exception`() = runTest {
        api.error = IOException("offline")

        val result = mediator.load(LoadType.REFRESH, state())

        assertThat(result).isInstanceOf(RemoteMediator.MediatorResult.Error::class.java)
        assertThat((result as RemoteMediator.MediatorResult.Error).throwable).isInstanceOf(IOException::class.java)
    }

    private fun state(pageSize: Int = 10) = PagingState<Int, FeedCatEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    /** [RemoteMediator.MediatorResult.Success] has no `equals`, so assert on the field. */
    private fun RemoteMediator.MediatorResult.endOfPaginationReached(): Boolean =
        (this as RemoteMediator.MediatorResult.Success).endOfPaginationReached
}
