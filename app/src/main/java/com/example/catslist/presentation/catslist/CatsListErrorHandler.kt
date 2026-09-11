package com.example.catslist.presentation.catslist

import com.example.catslist.R
import com.example.catslist.presentation.TextSource
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
        stateHolder.showError(TextSource.Res(R.string.catslist_error_loading_cats), retryable = true)
    }
}
