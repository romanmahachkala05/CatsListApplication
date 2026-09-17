package com.example.catslist.presentation.catslist

import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface ICatsListStateHolder : StateOwner<CatsListState> {
    fun showContent(cats: ImmutableList<Cat>)
    fun showLoading()
    fun showError(message: UiText)
    fun reset()
}

@ViewModelScoped
internal class CatsListStateHolder @Inject constructor() : ICatsListStateHolder {

    private val _state = MutableStateFlow(CatsListState())
    override val state: StateFlow<CatsListState> = _state.asStateFlow()

    override fun showContent(cats: ImmutableList<Cat>) = _state.update { current ->
        // An empty emission carries no evidence anything changed, so it must not flash empty
        // Content over Loading, nor silently clear an Error an unrelated feed emission (e.g.
        // favorites changing) didn't actually fix. Non-empty cats mean fetchNextCats() really
        // did append something, which is genuine recovery either way.
        val status = when {
            cats.isNotEmpty() -> CatsListUiStatus.Content
            current.status is CatsListUiStatus.Loading -> CatsListUiStatus.Loading
            current.status is CatsListUiStatus.Error -> current.status
            else -> CatsListUiStatus.Content
        }
        current.copy(status = status, cats = cats)
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
