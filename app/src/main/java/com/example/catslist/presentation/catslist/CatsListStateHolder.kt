package com.example.catslist.presentation.catslist

import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.TextSource
import com.example.catslist.domain.model.Cat
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface ICatsListStateHolder : StateOwner<CatsListState> {
    fun showContent(cats: List<Cat>)
    fun showError(message: TextSource, retryable: Boolean)
    fun reset()
}

@ViewModelScoped
class CatsListStateHolder @Inject constructor() : ICatsListStateHolder {

    private val _state = MutableStateFlow(CatsListState())
    override val state: StateFlow<CatsListState> = _state.asStateFlow()

    override fun showContent(cats: List<Cat>) = _state.update { current ->
        // The feed starts empty and fills in as fetches complete — don't flash
        // an empty Content state before the first cat actually arrives.
        val status = if (cats.isEmpty() && current.status is CatsListUiStatus.Loading) {
            CatsListUiStatus.Loading
        } else {
            CatsListUiStatus.Content
        }
        current.copy(status = status, cats = cats.toPersistentList())
    }

    override fun showError(message: TextSource, retryable: Boolean) = _state.update {
        it.copy(status = CatsListUiStatus.Error(message, retryable))
    }

    override fun reset() = _state.update { CatsListState() }
}
