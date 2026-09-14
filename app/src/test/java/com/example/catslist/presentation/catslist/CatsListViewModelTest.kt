package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.FetchNextCatsUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.UiText
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.FakeSnackbarNotifier
import com.example.catslist.testing.MainDispatcherRule
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CatsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatRepository()
    private val downloader = FakeImageDownloader()
    private val notifier = FakeSnackbarNotifier()

    @Test
    fun `loads a first page as soon as it is created`() = runTest {
        repository.enqueueBatch(cat("1"), cat("2"))

        val viewModel = viewModel()

        assertThat(repository.fetchCount).isEqualTo(1)
        assertThat(viewModel.state.value.status).isEqualTo(CatsListUiStatus.Content)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `stays loading while the first page is still empty`() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(CatsListUiStatus.Loading)
    }

    @Test
    fun `a failed first load becomes a retryable error`() = runTest {
        repository.fetchError = IOException("offline")

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(
            CatsListUiStatus.Error(UiText.Resource(R.string.catslist_error_loading_cats), retryable = true),
        )
    }

    @Test
    fun `a feed that ends in an error is shown, not thrown`() = runTest {
        // An exception escaping viewModelScope reaches the default handler and kills the process.
        repository.feedError = IOException("database is corrupt")

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(
            CatsListUiStatus.Error(UiText.Resource(R.string.catslist_error_feed_stopped), retryable = false),
        )
    }

    @Test
    fun `a dead feed offers no retry, since the stream will not emit again`() = runTest {
        repository.feedError = IOException("database is corrupt")

        val viewModel = viewModel()

        assertThat((viewModel.state.value.status as CatsListUiStatus.Error).retryable).isFalse()
    }

    @Test
    fun `LoadMore appends the next page`() = runTest {
        repository.enqueueBatch(cat("1"))
        repository.enqueueBatch(cat("2"))
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.LoadMore)

        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `LoadMore recovers the screen after a failed load`() = runTest {
        repository.fetchError = IOException("offline")
        val viewModel = viewModel()
        repository.fetchError = null
        repository.enqueueBatch(cat("1"))

        viewModel.onEvent(CatsListEvent.LoadMore)

        assertThat(viewModel.state.value.status).isEqualTo(CatsListUiStatus.Content)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `ToggleFavorite marks the cat in the feed`() = runTest {
        repository.enqueueBatch(cat("1"), cat("2"))
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("2")))

        val cats = viewModel.state.value.cats
        assertThat(cats.single { it.id == "2" }.isFavorite).isTrue()
        assertThat(cats.single { it.id == "1" }.isFavorite).isFalse()
    }

    @Test
    fun `ToggleFavorite unmarks a cat that was already favorited`() = runTest {
        repository.enqueueBatch(cat("1"))
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(viewModel.state.value.cats.single().isFavorite).isFalse()
    }

    @Test
    fun `a failed favorite toggle is reported instead of crashing the screen`() = runTest {
        repository.enqueueBatch(cat("1"))
        val viewModel = viewModel()
        repository.favoriteError = IOException("database is locked")

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_favorite_failed_message))
        assertThat(viewModel.state.value.status).isEqualTo(CatsListUiStatus.Content)
    }

    @Test
    fun `a cancelled favorite toggle is not reported as a failure`() = runTest {
        repository.favoriteError = CancellationException("screen left")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun `a cancelled load is not shown as an error`() = runTest {
        // Leaving the screen mid-fetch cancels the load; that is not something to
        // put a "couldn't load" message on screen for.
        repository.fetchError = CancellationException("screen left")

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(CatsListUiStatus.Loading)
    }

    @Test
    fun `Download hands the cat to the downloader and says so`() = runTest {
        val cat = cat("1")
        repository.enqueueBatch(cat)
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.Download(cat))

        assertThat(downloader.downloaded).containsExactly(cat.url to cat.id)
        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_download_started_message))
    }

    @Test
    fun `a failed download is reported instead of crashing the screen`() = runTest {
        downloader.error = IOException("no storage")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.Download(cat("1")))

        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_download_failed_message))
        assertThat(viewModel.state.value.status).isNotInstanceOf(CatsListUiStatus.Error::class.java)
    }

    @Test
    fun `a cancelled download is not reported as a failure`() = runTest {
        // Cancellation means the screen went away, not that the download broke — showing a
        // "couldn't download" Snackbar for it would be a lie, and would swallow the cancellation.
        downloader.error = CancellationException("screen left")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.Download(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    private fun viewModel(): CatsListViewModel {
        val stateHolder = CatsListStateHolder()
        return CatsListViewModel(
            stateHolder = stateHolder,
            errorHandler = CatsListErrorHandler(stateHolder),
            getCatFeed = GetCatFeedUseCase(repository),
            fetchNextCats = FetchNextCatsUseCase(repository),
            toggleFavorite = ToggleFavoriteUseCase(repository),
            downloadCatImage = DownloadCatImageUseCase(downloader),
            notifier = notifier,
        )
    }
}
