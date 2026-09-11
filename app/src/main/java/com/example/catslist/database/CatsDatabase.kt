package com.example.catslist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.catslist.models.CatDatabaseEntity

@Database(entities = [CatDatabaseEntity::class], version = 2)
abstract class CatsDatabase : RoomDatabase() {
    abstract fun catsDao(): CatsDao
}
