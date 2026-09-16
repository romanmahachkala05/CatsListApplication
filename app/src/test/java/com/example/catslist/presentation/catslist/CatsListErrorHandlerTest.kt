package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.presentation.UiText
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Test

class CatsListErrorHandlerTest {

    private val stateHolder = CatsListStateHolder()
    private val errorHandler = CatsListErrorHandler(stateHolder)

    @Test
    fun `a load failure becomes an error on screen`() {
        errorHandler.onLoadFailure(IOException("offline"))

        assertThat(stateHolder.state.value.status).isEqualTo(
            CatsListUiStatus.Error(UiText.Resource(R.string.catslist_error_loading_cats)),
        )
    }

    @Test
    fun `a dead feed gets its own message`() {
        errorHandler.onFeedFailure(IOException("database is corrupt"))

        assertThat(stateHolder.state.value.status).isEqualTo(
            CatsListUiStatus.Error(UiText.Resource(R.string.catslist_error_feed_stopped)),
        )
    }

    @Test
    fun `the same message covers any load failure, so no exception detail leaks to the user`() {
        errorHandler.onLoadFailure(IllegalStateException("boom"))
        val fromIllegalState = stateHolder.state.value.status

        errorHandler.onLoadFailure(IOException("offline"))

        assertThat(stateHolder.state.value.status).isEqualTo(fromIllegalState)
    }
}
