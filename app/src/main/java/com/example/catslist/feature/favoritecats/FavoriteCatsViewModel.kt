package com.example.catslist.feature.favoritecats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.core.download.CatImageDownloader
import com.example.catslist.core.mvi.StateOwner
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteCatsViewModel @Inject constructor(
    private val stateHolder: IFavoriteCatsStateHolder,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val imageDownloader: CatImageDownloader,
) : ViewModel(), StateOwner<FavoriteCatsState> by stateHolder {

    init {
        getFavoriteCats()
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: FavoriteCatsEvent) {
        when (event) {
            is FavoriteCatsEvent.RemoveFavorite -> viewModelScope.launch { removeFavorite(event.cat) }
            is FavoriteCatsEvent.Download -> imageDownloader.download(event.cat.url, event.cat.id)
        }
    }
}
