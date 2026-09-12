package com.example.catslist.presentation.favoritecats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.TextSource
import com.example.catslist.presentation.UiNotifier
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
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
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: UiNotifier,
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
            is FavoriteCatsEvent.Download -> download(event.cat)
        }
    }

    private fun download(cat: Cat) {
        viewModelScope.launch {
            val message = runCatching { downloadCatImage(cat) }
                .fold(
                    onSuccess = { TextSource.Res(R.string.common_download_started_message) },
                    onFailure = { TextSource.Res(R.string.common_download_failed_message) },
                )
            notifier.showMessage(message)
        }
    }
}
