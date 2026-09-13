package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

interface ICatsListErrorHandler {
    fun onLoadFailure(error: Throwable)
}

@ViewModelScoped
class CatsListErrorHandler @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onLoadFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.catslist_error_loading_cats), retryable = true)
    }
}
