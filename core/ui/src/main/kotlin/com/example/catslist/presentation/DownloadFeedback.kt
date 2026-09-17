package com.example.catslist.presentation

import androidx.lifecycle.ViewModel
import com.example.catslist.core.ui.R
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import kotlinx.coroutines.Job

/** Messages shared by every screen that downloads or favorites a cat, so the wording stays one thing. */
val FAVORITE_FAILED = UiText.Resource(R.string.common_favorite_failed_message)
val DOWNLOAD_FAILED = UiText.Resource(R.string.common_download_failed_message)
val DOWNLOAD_STARTED = UiText.Resource(R.string.common_download_started_message)

/** Downloads [cat]'s image and reports the start/failure via [notifier] — the one thing every screen does. */
fun ViewModel.downloadCat(
    cat: Cat,
    downloadCatImage: DownloadCatImageUseCase,
    notifier: SnackbarNotifier,
): Job = launchCatching(onFailure = { notifier.showMessage(DOWNLOAD_FAILED) }) {
    downloadCatImage(cat)
    notifier.showMessage(DOWNLOAD_STARTED)
}
