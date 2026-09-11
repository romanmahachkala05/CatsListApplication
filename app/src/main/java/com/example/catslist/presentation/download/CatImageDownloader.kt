package com.example.catslist.presentation.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.catslist.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Kicks off a system download of a cat image. The one place that talks to [DownloadManager]. */
class CatImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun download(url: String, catId: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(context.getString(R.string.download_notification_title, catId))
            setDescription(context.getString(R.string.download_notification_description))
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "cat_$catId.jpg")
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            allowScanningByMediaScanner()
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, context.getString(R.string.download_started_message), Toast.LENGTH_LONG).show()
    }
}
