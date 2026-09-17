package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FetchNextCatsUseCaseTest {

    private val repository = FakeCatRepository()
    private val fetchNextCats = FetchNextCatsUseCase(repository)

    @Test
    fun `appends the next batch to the feed`() = runTest {
        repository.enqueueBatch(cat("1"))
        repository.enqueueBatch(cat("2"))

        fetchNextCats()
        fetchNextCats()

        assertThat(repository.feed.first().map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test(expected = IOException::class)
    fun `lets a repository failure propagate to the caller`() = runTest {
        repository.fetchError = IOException("offline")

        fetchNextCats()
    }
}
