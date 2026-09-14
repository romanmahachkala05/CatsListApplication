package com.example.catslist.data.di

import com.example.catslist.data.remote.CatApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

private const val CAT_API_BASE_URL = "https://api.thecatapi.com"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * kotlinx.serialization rejects an unknown key by default, where Gson silently dropped it.
     * The search endpoint currently returns exactly the fields
     * [com.example.catslist.data.remote.CatDto] declares, so nothing breaks today — but that is
     * not a contract, and a field added upstream would otherwise start failing every response.
     * Tolerating unknown keys is the safe default for a wire model we do not own.
     */
    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(CAT_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    fun provideCatApiService(retrofit: Retrofit): CatApiService =
        retrofit.create(CatApiService::class.java)
}
