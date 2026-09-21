package com.example.catslist.data.di

import com.example.catslist.data.remote.CatApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val CAT_API_BASE_URL = "https://api.thecatapi.com"

/** Past this, a request is reported as a timeout rather than left hanging behind a spinner. */
private const val TIMEOUT_SECONDS = 15L

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * kotlinx.serialization rejects unknown keys by default, so a field added upstream would
     * start failing every response. Tolerating them is the safe default for a wire model we
     * do not own.
     */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The app's one client, shared with Coil through `App` (ADR-0026). Sharing is what makes
     * the connection pool and thread pool shared too — the feed's JSON and its images go to
     * hosts this app talks to constantly, so a warm pool is most of the win.
     *
     * OkHttp's own defaults are 10s connect / 10s read / 10s write, but `callTimeout` is 0 —
     * no cap at all on an end-to-end call, including retries and redirects. That is the one
     * worth setting: without it a request that keeps almost-progressing never fails, and the
     * feed shows a spinner with nothing behind it.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(CAT_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    fun provideCatApiService(retrofit: Retrofit): CatApiService = retrofit.create(CatApiService::class.java)
}
