package com.example.catslist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {

    @Insert
    suspend fun insertCat(cat: CatEntity)

    @Delete
    suspend fun deleteCat(cat: CatEntity)

    @Query("SELECT * FROM favoriteCatsTable")
    fun getAllCats(): Flow<List<CatEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoriteCatsTable WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Query("DELETE FROM favoriteCatsTable")
    suspend fun deleteAllCats()
}
