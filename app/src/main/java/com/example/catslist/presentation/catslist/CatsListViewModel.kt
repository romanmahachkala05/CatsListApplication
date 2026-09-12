package com.example.catslist.presentation.catslist

import android.util.Log
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
import kotlinx.coroutines.CancellationException
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
            try {
                downloadCatImage(cat)
                notifier.showMessage(TextSource.Res(R.string.common_download_started_message))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download cat ${cat.id}", e)
                notifier.showMessage(TextSource.Res(R.string.common_download_failed_message))
            }
        }
    }

    private companion object {
        const val TAG = "CatsListViewModel"
    }
}
