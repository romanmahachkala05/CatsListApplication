package com.example.catslist.presentation.catslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.TextSource
import com.example.catslist.presentation.UiNotifier
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.FetchNextCatsUseCase
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
    private val fetchNextCats: FetchNextCatsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: UiNotifier,
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
            is CatsListEvent.Download -> download(event.cat)
        }
    }

    private fun loadMore() {
        viewModelScope.launch {
            runCatching { fetchNextCats() }
                .onFailure(errorHandler::onLoadFailure)
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
