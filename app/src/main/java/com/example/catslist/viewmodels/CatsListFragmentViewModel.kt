package com.example.catslist.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.FetchNextCatUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatsListFragmentViewModel @Inject constructor(
    getCatFeed: GetCatFeedUseCase,
    private val fetchNextCat: FetchNextCatUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    val cats: StateFlow<List<Cat>> = getCatFeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCat() {
        viewModelScope.launch { fetchNextCat() }
    }

    fun onFavoriteButtonClick(cat: Cat) {
        viewModelScope.launch { toggleFavorite(cat) }
    }

    fun downloadCatImage(context: Context, url: String, catId: String) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Image of cat with id $catId")
        request.setDescription("Downloading image..")
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "cat_$catId.jpg")
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.allowScanningByMediaScanner()
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Downloading started", Toast.LENGTH_LONG).show()
    }

}
