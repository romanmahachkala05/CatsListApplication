package com.example.catslist.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database (
    version = 1,
    entities = [
        CatDatabaseEntity::class
    ]
        )
abstract class CatsDatabase: RoomDatabase() {

}