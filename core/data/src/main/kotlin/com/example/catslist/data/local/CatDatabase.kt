package com.example.catslist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CatEntity::class, FeedCatEntity::class, FeedRemoteKeysEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
    abstract fun catFeedDao(): CatFeedDao
}
