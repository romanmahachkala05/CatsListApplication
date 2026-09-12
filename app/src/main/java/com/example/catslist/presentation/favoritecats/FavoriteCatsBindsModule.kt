package com.example.catslist.presentation.favoritecats

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class FavoriteCatsBindsModule {

    @Binds
    abstract fun bindStateHolder(impl: FavoriteCatsStateHolder): IFavoriteCatsStateHolder
}
