package com.example.catslist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CatEntity::class], version = 3, exportSchema = true)
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
}
