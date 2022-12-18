package com.example.catslist.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.catslist.models.Cat


@Dao
interface CatsDao {

    @Insert
    suspend fun insertCat(cat: Cat)

    @Query("SELECT * FROM favoriteCats")
    suspend fun getAll(): List<Cat>

}