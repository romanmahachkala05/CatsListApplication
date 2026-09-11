package com.example.catslist.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.R
import com.example.catslist.database.CatsDatabaseRepository
import com.example.catslist.models.Cat
import com.example.catslist.models.CatDatabaseEntity
import com.example.catslist.tools.CatStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatsListFragmentViewModel @Inject constructor(
    private val catsDatabaseRepository: CatsDatabaseRepository
) : ViewModel() {

    private val tag = "CatsListFragmentViewModel"

    init {
        Log.v(tag, "init")
        viewModelScope.launch {
            catsDatabaseRepository.getAllCats().forEach { CatStorage.favoriteCats.add(it.toCat()) }
            Log.v(
                tag,
                "getAllCats, add each to CatStorage.favoriteCats = ${CatStorage.favoriteCats}"
            )
            CatStorage.notifyFavChanges()
        }
    }

    fun addCat() {
        CatStorage.getNewCatData()
    }

    fun onFavoriteButtonClick(cat: Cat, view: View) {
        viewModelScope.launch {
            val icon = view.findViewById<AppCompatImageButton>(R.id.item_cat_star_button)
            if (!(CatStorage.favoriteCats.any { it.id == cat.id })) {
                cat.favorite = true
                CatStorage.favoriteCats.add(cat)
                CatStorage.notifyFavChanges()
                catsDatabaseRepository.insert(CatDatabaseEntity.fromCat(cat))
                icon.setBackgroundResource(R.drawable.ic_star_filled)
            } else {
                icon.setBackgroundResource(R.drawable.ic_star_empty)
                CatStorage.favoriteCats.removeIf { it.id == cat.id }
                CatStorage.notifyFavChanges()
                catsDatabaseRepository.delete(CatDatabaseEntity.fromCat(cat))

            }
        }
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
