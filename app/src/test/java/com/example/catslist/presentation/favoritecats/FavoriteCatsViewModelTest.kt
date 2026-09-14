package com.example.catslist.presentation.favoritecats

import com.example.catslist.R
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
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

class FavoriteCatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatRepository()
    private val downloader = FakeImageDownloader()
    private val notifier = FakeSnackbarNotifier()

    @Test
    fun `shows the stored favorites as soon as it is created`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))

        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Content)
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `shows the empty state when nothing is favorited`() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
    }

    @Test
    fun `RemoveFavorite drops the cat from the screen`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("2")
    }

    @Test
    fun `removing the last favorite goes back to the empty state`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(viewModel.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
    }

    @Test
    fun `a failed removal is reported instead of crashing the screen`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()
        repository.favoriteError = IOException("database is locked")

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_favorite_failed_message))
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `a cancelled removal is not reported as a failure`() = runTest {
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()
        repository.favoriteError = CancellationException("screen left")

        viewModel.onEvent(FavoriteCatsEvent.RemoveFavorite(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    @Test
    fun `Download hands the cat to the downloader and says so`() = runTest {
        val cat = cat("1", isFavorite = true)
        repository.setFavorites(cat)
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat))

        assertThat(downloader.downloaded).containsExactly(cat.url to cat.id)
        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_download_started_message))
    }

    @Test
    fun `a failed download is reported instead of crashing the screen`() = runTest {
        downloader.error = IOException("no storage")
        repository.setFavorites(cat("1"))
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat("1")))

        assertThat(notifier.shown).containsExactly(UiText.Resource(R.string.common_download_failed_message))
        assertThat(viewModel.state.value.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `a cancelled download is not reported as a failure`() = runTest {
        downloader.error = CancellationException("screen left")
        val viewModel = viewModel()

        viewModel.onEvent(FavoriteCatsEvent.Download(cat("1")))

        assertThat(notifier.shown).isEmpty()
    }

    private fun viewModel() = FavoriteCatsViewModel(
        stateHolder = FavoriteCatsStateHolder(),
        getFavoriteCats = GetFavoriteCatsUseCase(repository),
        removeFavorite = RemoveFavoriteUseCase(repository),
        downloadCatImage = DownloadCatImageUseCase(downloader),
        notifier = notifier,
    )
}
