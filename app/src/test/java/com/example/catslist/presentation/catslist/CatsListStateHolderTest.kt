package com.example.catslist.presentation.catslist

import com.example.catslist.presentation.UiText
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CatsListStateHolderTest {

    private val stateHolder = CatsListStateHolder()

    @Test
    fun `starts loading with no cats`() {
        assertThat(stateHolder.state.value).isEqualTo(CatsListState())
        assertThat(stateHolder.state.value.status).isEqualTo(CatsListUiStatus.Loading)
    }

    @Test
    fun `showing cats switches to content`() {
        stateHolder.showContent(listOf(cat("1"), cat("2")))

        val state = stateHolder.state.value
        assertThat(state.status).isEqualTo(CatsListUiStatus.Content)
        assertThat(state.cats.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `an empty first emission stays loading rather than flashing empty content`() {
        // The feed flow emits its empty initial value before the first fetch completes.
        stateHolder.showContent(emptyList())

        assertThat(stateHolder.state.value.status).isEqualTo(CatsListUiStatus.Loading)
        assertThat(stateHolder.state.value.cats).isEmpty()
    }

    @Test
    fun `an empty emission after content is content, not loading`() {
        stateHolder.showContent(listOf(cat("1")))

        stateHolder.showContent(emptyList())

        assertThat(stateHolder.state.value.status).isEqualTo(CatsListUiStatus.Content)
        assertThat(stateHolder.state.value.cats).isEmpty()
    }

    @Test
    fun `showing an error keeps the cats already on screen`() {
        stateHolder.showContent(listOf(cat("1")))

        stateHolder.showError(MESSAGE)

        val state = stateHolder.state.value
        assertThat(state.status).isEqualTo(CatsListUiStatus.Error(MESSAGE))
        assertThat(state.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `going back to loading keeps the cats already on screen`() {
        stateHolder.showContent(listOf(cat("1")))
        stateHolder.showError(MESSAGE)

        stateHolder.showLoading()

        val state = stateHolder.state.value
        assertThat(state.status).isEqualTo(CatsListUiStatus.Loading)
        assertThat(state.cats.map { it.id }).containsExactly("1")
    }

    @Test
    fun `reset returns to the initial state`() {
        stateHolder.showContent(listOf(cat("1")))
        stateHolder.showError(MESSAGE)

        stateHolder.reset()

        assertThat(stateHolder.state.value).isEqualTo(CatsListState())
    }

    private companion object {
        val MESSAGE = UiText.Raw("Couldn't load a cat")
    }
}
