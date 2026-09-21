package com.example.catslist.presentation.catslist

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test

class CatsListErrorHandlerTest {

    private val stateHolder = CatsListStateHolder()
    private val errorHandler = CatsListErrorHandler(stateHolder)

    @Test
    fun `a broken favorites stream marks the overlay unavailable`() {
        errorHandler.onFavoriteIdsFailure(IOException("database is corrupt"))

        assertThat(stateHolder.state.value.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Unavailable)
    }

    @Test
    fun `the failure does not clear the ids the feed is already rendering`() {
        stateHolder.showFavorites(persistentSetOf("1"))

        errorHandler.onFavoriteIdsFailure(IOException("database is corrupt"))

        assertThat(stateHolder.state.value.favoriteIds).containsExactly("1")
    }
}
