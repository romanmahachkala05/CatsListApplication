package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCatFeedUseCaseTest {

    private val repository = FakeCatRepository()
    private val getCatFeed = GetCatFeedUseCase(repository)

    @Test
    fun `emits the repository feed`() = runTest {
        repository.enqueueBatch(cat("1"), cat("2"))
        repository.fetchNextBatch()

        assertThat(getCatFeed().first().map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `reflects favorite status from the repository`() = runTest {
        repository.enqueueBatch(cat("1"), cat("2"))
        repository.fetchNextBatch()
        repository.toggleFavorite(cat("2"))

        val feed = getCatFeed().first()

        assertThat(feed.single { it.id == "2" }.isFavorite).isTrue()
        assertThat(feed.single { it.id == "1" }.isFavorite).isFalse()
    }
}
