package com.example.catslist.presentation.favoritecats

import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import com.example.catslist.domain.model.Cat
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface IFavoriteCatsStateHolder : StateOwner<FavoriteCatsState> {
    fun showFavorites(cats: List<Cat>)
    fun showError(message: UiText)
    fun reset()
}

@ViewModelScoped
class FavoriteCatsStateHolder @Inject constructor() : IFavoriteCatsStateHolder {

    private val _state = MutableStateFlow(FavoriteCatsState())
    override val state: StateFlow<FavoriteCatsState> = _state.asStateFlow()

    override fun showFavorites(cats: List<Cat>) = _state.update {
        it.copy(
            status = if (cats.isEmpty()) FavoriteCatsUiStatus.Empty else FavoriteCatsUiStatus.Content,
            cats = cats.toPersistentList(),
        )
    }

    override fun showError(message: UiText) = _state.update {
        it.copy(status = FavoriteCatsUiStatus.Error(message))
    }

    override fun reset() = _state.update { FavoriteCatsState() }
}
