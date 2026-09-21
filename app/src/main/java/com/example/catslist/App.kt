package com.example.catslist

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * Also the place Coil's singleton loader is built, so the app has exactly one [OkHttpClient]
 * rather than one per library (ADR-0026). Wiring a singleton across two libraries is
 * composition-root work, which is why it lives here and not in `:core:designsystem`.
 */
@HiltAndroidApp
class App :
    Application(),
    SingletonImageLoader.Factory {

    /**
     * [Lazy], so the client — and the thread and connection pools it opens — is built on
     * whichever background thread first needs it, not during `onCreate` on the main thread.
     */
    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    /**
     * Registering the fetcher explicitly is what removes the second client. `coil-network-okhttp`
     * also registers one through a `ServiceLoader`, built over an `OkHttpClient()` of its own —
     * but `RealImageLoader` assembles the builder's own components ahead of the ServiceLoader's,
     * so this one is matched first for every http(s) request and that default is never built.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
        }
        .build()
}
