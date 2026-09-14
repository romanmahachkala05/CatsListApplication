package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.presentation.UiText
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class CatsListErrorHandlerTest {

    private val stateHolder = CatsListStateHolder()
    private val errorHandler = CatsListErrorHandler(stateHolder)

    @Test
    fun `a load failure becomes a retryable error on screen`() {
        errorHandler.onLoadFailure(IOException("offline"))

        assertThat(stateHolder.state.value.status).isEqualTo(
            CatsListUiStatus.Error(
                message = UiText.Resource(R.string.catslist_error_loading_cats),
                retryable = true,
            ),
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
