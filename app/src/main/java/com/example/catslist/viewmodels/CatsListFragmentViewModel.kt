package com.example.catslist.viewmodels

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.AndroidViewModel
import com.example.catslist.R
import com.example.catslist.models.Cat
import com.example.catslist.models.CatDatabaseEntity
import com.example.catslist.database.CatsDatabaseRepository
import com.example.catslist.tools.CatStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class CatsListFragmentViewModel(app: Application) : AndroidViewModel(app) {


    private val catsDatabaseRepository = CatsDatabaseRepository(app)
    private val tag = "CatsListFragmentViewModel"

    init {
        Log.v(tag, "init")
        CoroutineScope(Main).launch {
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
        CoroutineScope(Main).launch {
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