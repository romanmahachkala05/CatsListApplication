package com.example.catslist.presentation.favoritecats

import com.example.catslist.presentation.UiText
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FavoriteCatsStateHolderTest {

    private val stateHolder = FavoriteCatsStateHolder()

    @Test
    fun `starts loading with no cats`() {
        assertThat(stateHolder.state.value).isEqualTo(FavoriteCatsState())
        assertThat(stateHolder.state.value.status).isEqualTo(FavoriteCatsUiStatus.Loading)
    }

    @Test
    fun `showing favorites switches to content`() {
        stateHolder.showFavorites(listOf(cat("1", isFavorite = true)))

        val state = stateHolder.state.value
        assertThat(state.status).isEqualTo(FavoriteCatsUiStatus.Content)
        assertThat(state.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `no favorites is empty, not loading`() {
        // Unlike the feed, an empty favorites table is a real answer, not a pending one.
        stateHolder.showFavorites(emptyList())

        assertThat(stateHolder.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
    }

    @Test
    fun `removing the last favorite goes back to empty`() {
        stateHolder.showFavorites(listOf(cat("1", isFavorite = true)))

        stateHolder.showFavorites(emptyList())

        assertThat(stateHolder.state.value.status).isEqualTo(FavoriteCatsUiStatus.Empty)
        assertThat(stateHolder.state.value.cats).isEmpty()
    }

    @Test
    fun `showing an error keeps the favorites already on screen`() {
        stateHolder.showFavorites(listOf(cat("1", isFavorite = true)))

        stateHolder.showError(MESSAGE)

        val state = stateHolder.state.value
        assertThat(state.status).isEqualTo(FavoriteCatsUiStatus.Error(MESSAGE))
        assertThat(state.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `reset returns to the initial state`() {
        stateHolder.showFavorites(listOf(cat("1", isFavorite = true)))

        stateHolder.reset()

        assertThat(stateHolder.state.value).isEqualTo(FavoriteCatsState())
    }

    private companion object {
        val MESSAGE = UiText.Raw("Something went wrong")
    }
}
