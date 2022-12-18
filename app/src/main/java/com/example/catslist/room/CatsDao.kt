package com.example.catslist.room

import androidx.room.*


@Dao
interface CatsDao {

    @Insert
    suspend fun insertCat(catsDbEntity: CatDatabaseEntity)

    @Delete
    suspend fun deleteCat(catsDbEntity: CatDatabaseEntity)

    @Update
    suspend fun updateCat(catsDbEntity: CatDatabaseEntity)

    @Query("SELECT * FROM favoriteCatsTable")
    fun getAllCats(): List<CatDatabaseEntity>

}