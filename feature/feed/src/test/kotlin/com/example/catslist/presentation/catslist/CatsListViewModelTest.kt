package com.example.catslist.presentation.catslist

import androidx.paging.testing.asSnapshot
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.DOWNLOAD_FAILED
import com.example.catslist.presentation.DOWNLOAD_STARTED
import com.example.catslist.presentation.FAVORITE_FAILED
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.FakeImageDownloader
import com.example.catslist.testing.FakeSnackbarNotifier
import com.example.catslist.testing.MainDispatcherRule
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Loading/error/retry for the feed itself is Paging's own state machine now (see
 * [CatsListViewModel.pagedCats]'s doc) — proven by [CatFeedRemoteMediatorTest] and
 * [com.example.catslist.data.local.CatFeedDaoTest], not here. What is still this
 * ViewModel's job: forwarding the paged feed and keeping the live favorite-id set in
 * [CatsListState] (never combined into the feed itself — see `CatRepositoryImpl.feed`'s doc and
 * ADR-0023), and handling favorite/download events.
 */
class CatsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatRepository()
    private val downloader = FakeImageDownloader()
    private val notifier = FakeSnackbarNotifier()
    private val stateHolder = CatsListStateHolder()

    @Test
    fun `exposes the repository feed`() = runTest {
        repository.setFeed(cat("1"), cat("2"))
        val viewModel = viewModel()

        val snapshot = viewModel.pagedCats.asSnapshot()

        assertThat(snapshot.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `favoriteIds reflects favorites, and ToggleFavorite adds to it`() = runTest {
        repository.setFeed(cat("1"), cat("2"))
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("2")))

        assertThat(viewModel.state.value.favoriteIds).containsExactly("2")
    }

    @Test
    fun `ToggleFavorite removes an id already favorited`() = runTest {
        repository.setFeed(cat("1"))
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(viewModel.state.value.favoriteIds).isEmpty()
    }

    @Test
    fun `a broken favorites stream marks favorites unavailable and leaves the feed alone`() = runTest {
        // Before this guard existed the throw escaped viewModelScope, which on Android kills
        // the process — the feed itself loads independently and is unaffected.
        repository.setFeed(cat("1"))
        repository.favoritesError = IOException("database is corrupt")

        val viewModel = viewModel()

        assertThat(viewModel.state.value.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Unavailable)
        assertThat(viewModel.pagedCats.asSnapshot().map { it.id }).containsExactly("1")
    }

    @Test
    fun `a failed favorite toggle is reported instead of crashing the screen`() = runTest {
        repository.setFeed(cat("1"))
        val viewModel = viewModel()
        repository.favoriteError = IOException("database is locked")

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(notifier.shown).containsExactly(FAVORITE_FAILED)
    }

    @Test
    fun `a cancelled favorite toggle is not reported as a failure`() = runTest {
        repository.favoriteError = CancellationException("screen left")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.ToggleFavorite(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun `Download hands the cat to the downloader and says so`() = runTest {
        val cat = cat("1")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.Download(cat))

        assertThat(downloader.downloaded).containsExactly(cat.url to cat.id)
        assertThat(notifier.shown).containsExactly(DOWNLOAD_STARTED)
    }

    @Test
    fun `a failed download is reported instead of crashing the screen`() = runTest {
        downloader.error = IOException("no storage")
        val viewModel = viewModel()

        viewModel.onEvent(CatsListEvent.Download(cat("1")))

        assertThat(notifier.shown).containsExactly(DOWNLOAD_FAILED)
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

    private fun viewModel() = CatsListViewModel(
        stateHolder = stateHolder,
        errorHandler = CatsListErrorHandler(stateHolder),
        getCatFeed = GetCatFeedUseCase(repository),
        getFavoriteCats = GetFavoriteCatsUseCase(repository),
        toggleFavorite = ToggleFavoriteUseCase(repository),
        downloadCatImage = DownloadCatImageUseCase(downloader),
        notifier = notifier,
    )
}
