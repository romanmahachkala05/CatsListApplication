package com.example.catslist.viewmodels

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.catslist.models.Cat
import com.example.catslist.database.CatDatabaseEntity
import com.example.catslist.database.CatsDatabaseRepository
import com.example.catslist.tools.CatStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteCatsListFragmentViewModel(app: Application) : AndroidViewModel(app) {

    private val tag = "FavoriteCatsListFragmentViewModel"
    private val catsDatabaseRepository = CatsDatabaseRepository(app)

    fun onFavoriteButtonClick(cat: Cat) {
        CoroutineScope(Dispatchers.Main).launch {
            if (CatStorage.cats.any { it.id == cat.id}) {
                cat.favorite = false
                CatStorage.notifyChanges()
            }
            CatStorage.favoriteCats.remove(cat)
            CatStorage.notifyFavChanges()
            catsDatabaseRepository.delete(CatDatabaseEntity.fromCat(cat))
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