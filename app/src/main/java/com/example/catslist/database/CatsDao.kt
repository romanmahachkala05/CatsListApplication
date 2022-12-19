package com.example.catslist.database

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
    suspend fun getAllCats(): List<CatDatabaseEntity>

    @Query("DELETE FROM favoriteCatsTable")
    suspend fun deleteAllCats()

}