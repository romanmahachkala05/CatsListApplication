package com.example.catslist.di

import com.example.catslist.data.repository.CatRepositoryImpl
import com.example.catslist.domain.repository.CatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCatRepository(impl: CatRepositoryImpl): CatRepository
}
