package com.example.catslist.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CatDatabaseEntity::class], version = 1)
abstract class CatsDatabase : RoomDatabase() {
    abstract fun catsDao(): CatsDao
}