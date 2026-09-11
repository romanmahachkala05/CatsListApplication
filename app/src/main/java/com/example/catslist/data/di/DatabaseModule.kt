package com.example.catslist.data.di

import android.content.Context
import androidx.room.Room
import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.CatDatabase
import com.example.catslist.data.local.MIGRATION_2_3
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
    fun provideCatDatabase(@ApplicationContext context: Context): CatDatabase =
        Room.databaseBuilder(context, CatDatabase::class.java, "cats_database")
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideCatDao(database: CatDatabase): CatDao = database.catDao()
}
