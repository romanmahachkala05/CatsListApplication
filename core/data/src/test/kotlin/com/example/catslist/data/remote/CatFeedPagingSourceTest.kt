package com.example.catslist.data.remote

import androidx.paging.PagingSource
import com.example.catslist.testing.FakeCatApiService
import com.example.catslist.testing.catDto
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CatFeedPagingSourceTest {

    private val api = FakeCatApiService()
    private val pagingSource = CatFeedPagingSource(api)

    @Test
    fun `the first load asks for page 0 and points at the next one`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(api.lastRequestedPage).isEqualTo(0)
        assertThat(page.data.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(page.nextKey).isEqualTo(1)
    }

    @Test
    fun `the feed only ever appends`() = runTest {
        api.enqueueResponse(catDto("1"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        // There is no signal for cats newer than the ones already held, so nothing is ever above
        // the first page and Paging must never be told to look.
        assertThat(page.prevKey).isNull()
    }

    @Test
    fun `an empty response ends pagination`() = runTest {
        api.enqueueResponse()

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(page.nextKey).isNull()
    }

    @Test
    fun `duplicates within one response are dropped`() = runTest {
        // TheCatAPI's search endpoint can hand back the same cat twice in one response.
        api.enqueueResponse(catDto("1"), catDto("1"), catDto("2"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(page.data.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `a cat already sent is not sent again by a later page`() = runTest {
        // The Room cache this replaced deduplicated across pages via its primary key. Nothing
        // does that now except this source, and the list keys its items by id — a repeat is a
        // crash rather than a cosmetic double (ADR-0015).
        api.enqueueResponse(catDto("1"), catDto("2"))
        api.enqueueResponse(catDto("2"), catDto("3"))

        val first = pagingSource.refresh() as PagingSource.LoadResult.Page
        val second = pagingSource.append(first.nextKey) as PagingSource.LoadResult.Page

        assertThat(second.data.map { it.id }).containsExactly("3")
    }

    @Test
    fun `a page of nothing but repeats still leads to the next page`() = runTest {
        api.enqueueResponse(catDto("1"))
        api.enqueueResponse(catDto("1"))

        val first = pagingSource.refresh() as PagingSource.LoadResult.Page
        val second = pagingSource.append(first.nextKey) as PagingSource.LoadResult.Page

        // Emptied by de-duplication, not by the API running out — stopping here would strand the
        // feed on a page the server still had more behind.
        assertThat(second.data).isEmpty()
        assertThat(second.nextKey).isEqualTo(2)
    }

    @Test
    fun `a network failure becomes a load error rather than an exception`() = runTest {
        api.error = IOException("offline")

        val result = pagingSource.refresh()

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
    }

    private suspend fun CatFeedPagingSource.refresh() =
        load(PagingSource.LoadParams.Refresh(key = null, loadSize = LOAD_SIZE, placeholdersEnabled = false))

    private suspend fun CatFeedPagingSource.append(key: Int?) = load(
        PagingSource.LoadParams.Append(
            key = requireNotNull(key),
            loadSize = LOAD_SIZE,
            placeholdersEnabled = false,
        ),
    )

    private companion object {
        const val LOAD_SIZE = 10
    }
}
