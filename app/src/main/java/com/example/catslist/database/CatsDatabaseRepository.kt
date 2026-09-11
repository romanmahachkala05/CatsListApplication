package com.example.catslist.database

import com.example.catslist.models.CatDatabaseEntity
import javax.inject.Inject

class CatsDatabaseRepository @Inject constructor(
    private val catsDao: CatsDao
) {

    suspend fun insert(cat: CatDatabaseEntity) {
        catsDao.insertCat(cat)
    }

    suspend fun delete(cat: CatDatabaseEntity) {
        catsDao.deleteCat(cat)
    }

    suspend fun update(cat: CatDatabaseEntity) {
        catsDao.updateCat(cat)
    }

    suspend fun getAllCats(): List<CatDatabaseEntity> {
        return catsDao.getAllCats()
    }

    suspend fun deleteAllCats() {
        catsDao.deleteAllCats()
    }

}
