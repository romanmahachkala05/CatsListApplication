package com.example.catslist.data.di

import com.example.catslist.data.download.CatImageDownloader
import com.example.catslist.data.network.ConnectivityNetworkMonitor
import com.example.catslist.data.repository.CatRepositoryImpl
import com.example.catslist.domain.ImageDownloader
import com.example.catslist.domain.NetworkMonitor
import com.example.catslist.domain.repository.CatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Domain interface → data implementation bindings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {

    @Binds
    abstract fun bindCatRepository(impl: CatRepositoryImpl): CatRepository

    @Binds
    abstract fun bindImageDownloader(impl: CatImageDownloader): ImageDownloader

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
