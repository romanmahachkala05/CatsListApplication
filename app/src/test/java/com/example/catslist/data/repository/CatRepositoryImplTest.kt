package com.example.catslist.data.repository

import com.example.catslist.testing.FakeCatApiService
import com.example.catslist.testing.FakeCatDao
import com.example.catslist.testing.cat
import com.example.catslist.testing.catDto
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class CatRepositoryImplTest {

    private val api = FakeCatApiService()
    private val dao = FakeCatDao()
    private val repository = CatRepositoryImpl(api, dao)

    @Test
    fun `feed starts empty`() = runTest {
        assertThat(repository.feed.first()).isEmpty()
    }

    @Test
    fun `fetching appends a whole page at a time`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"), catDto("3"))

        repository.fetchNextBatch()

        assertThat(repository.feed.first().map { it.id }).containsExactly("1", "2", "3").inOrder()
        assertThat(api.lastRequestedLimit).isEqualTo(PAGE_SIZE)
    }

    @Test
    fun `fetching again appends to what is already there`() = runTest {
        api.enqueueResponse(catDto("1"))
        api.enqueueResponse(catDto("2"))

        repository.fetchNextBatch()
        repository.fetchNextBatch()

        assertThat(repository.feed.first().map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `drops duplicates within one page`() = runTest {
        // The API's random-image endpoint can hand back the same cat twice in one response.
        api.enqueueResponse(catDto("1"), catDto("1"), catDto("2"))

        repository.fetchNextBatch()

        assertThat(repository.feed.first().map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `drops cats already in the feed from a later page`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"))
        api.enqueueResponse(catDto("2"), catDto("3"))

        repository.fetchNextBatch()
        repository.fetchNextBatch()

        assertThat(repository.feed.first().map { it.id }).containsExactly("1", "2", "3").inOrder()
    }

    @Test(expected = IOException::class)
    fun `lets a network failure propagate`() = runTest {
        api.error = IOException("offline")

        repository.fetchNextBatch()
    }

    @Test
    fun `feed marks cats that are favorited`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"))
        repository.fetchNextBatch()

        repository.toggleFavorite(cat("2"))

        val feed = repository.feed.first()
        assertThat(feed.single { it.id == "2" }.isFavorite).isTrue()
        assertThat(feed.single { it.id == "1" }.isFavorite).isFalse()
    }

    @Test
    fun `toggling an unfavorited cat stores it`() = runTest {
        repository.toggleFavorite(cat("1"))

        assertThat(repository.favorites.first().map { it.id }).containsExactly("1")
    }

    @Test
    fun `toggling a favorited cat removes it`() = runTest {
        repository.toggleFavorite(cat("1"))
        repository.toggleFavorite(cat("1"))

        assertThat(repository.favorites.first()).isEmpty()
    }

    @Test
    fun `toggling the same cat repeatedly ends where it started`() = runTest {
        // Each tap used to be a separate read-then-write, so a toggle could insert a row that
        // was already there — an ABORT on the primary key. It is one transactional call now.
        repeat(times = 6) { repository.toggleFavorite(cat("1")) }

        assertThat(repository.favorites.first()).isEmpty()
    }

    @Test
    fun `an odd number of toggles leaves the cat favorited`() = runTest {
        repeat(times = 5) { repository.toggleFavorite(cat("1")) }

        assertThat(repository.favorites.first().map { it.id }).containsExactly("1")
    }

    @Test
    fun `favorites come back marked as favorites`() = runTest {
        repository.toggleFavorite(cat("1"))

        assertThat(repository.favorites.first().single().isFavorite).isTrue()
    }

    @Test
    fun `removing a favorite leaves the others alone`() = runTest {
        repository.toggleFavorite(cat("1"))
        repository.toggleFavorite(cat("2"))

        repository.removeFavorite(cat("1"))

        assertThat(repository.favorites.first().map { it.id }).containsExactly("2")
    }

    @Test
    fun `unfavoriting keeps the cat in the feed, unmarked`() = runTest {
        api.enqueueResponse(catDto("1"))
        repository.fetchNextBatch()
        repository.toggleFavorite(cat("1"))

        repository.removeFavorite(cat("1"))

        assertThat(repository.feed.first().single().isFavorite).isFalse()
    }

    private companion object {
        /** Mirrors `CatRepositoryImpl.PAGE_SIZE`, which is private to the implementation. */
        const val PAGE_SIZE = 10
    }
}
