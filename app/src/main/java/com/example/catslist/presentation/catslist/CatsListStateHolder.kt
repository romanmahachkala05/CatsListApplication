package com.example.catslist.presentation.catslist

import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ICatsListStateHolder : StateOwner<CatsListState> {
    fun showContent(cats: List<Cat>)
    fun showLoading()
    fun showError(message: UiText)
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

    /** Keeps the cats already on screen: a retry redisplays them rather than starting blank. */
    override fun showLoading() = _state.update {
        it.copy(status = CatsListUiStatus.Loading)
    }

    override fun showError(message: UiText) = _state.update {
        it.copy(status = CatsListUiStatus.Error(message))
    }

    override fun reset() = _state.update { CatsListState() }
}
