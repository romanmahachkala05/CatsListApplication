package com.example.catslist.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CatDatabaseEntity::class], version = 2)
abstract class CatsDatabase : RoomDatabase() {
    abstract fun catsDao(): CatsDao

    companion object {
        private var instance: CatsDatabase? = null

        @Synchronized
        fun getInstance(context: Context): CatsDatabase {
            Log.v("starting building DB", "getInstance")
            if (instance == null) {
                Log.v("starting building DB", "puk")
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    CatsDatabase::class.java, "cats_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }
}