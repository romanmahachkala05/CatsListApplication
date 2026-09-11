package com.example.catslist.feature.catslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.core.download.CatImageDownloader
import com.example.catslist.core.mvi.StateOwner
import com.example.catslist.domain.usecase.FetchNextCatUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatsListViewModel @Inject constructor(
    private val stateHolder: ICatsListStateHolder,
    private val errorHandler: ICatsListErrorHandler,
    getCatFeed: GetCatFeedUseCase,
    private val fetchNextCat: FetchNextCatUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val imageDownloader: CatImageDownloader,
) : ViewModel(), StateOwner<CatsListState> by stateHolder {

    init {
        getCatFeed()
            .onEach(stateHolder::showContent)
            .launchIn(viewModelScope)
        loadMore()
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: CatsListEvent) {
        when (event) {
            CatsListEvent.LoadMore -> loadMore()
            is CatsListEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(event.cat) }
            is CatsListEvent.Download -> imageDownloader.download(event.cat.url, event.cat.id)
        }
    }

    private fun loadMore() {
        viewModelScope.launch {
            repeat(5) {
                runCatching { fetchNextCat() }
                    .onFailure(errorHandler::onLoadFailure)
            }
        }
    }
}
