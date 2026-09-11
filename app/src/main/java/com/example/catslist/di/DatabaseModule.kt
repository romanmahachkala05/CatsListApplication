package com.example.catslist.di

import android.content.Context
import androidx.room.Room
import com.example.catslist.database.CatsDao
import com.example.catslist.database.CatsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCatsDatabase(@ApplicationContext context: Context): CatsDatabase =
        Room.databaseBuilder(context, CatsDatabase::class.java, "cats_database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCatsDao(database: CatsDatabase): CatsDao = database.catsDao()
}
