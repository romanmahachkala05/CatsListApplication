package com.example.catslist.database

import android.app.Application

class CatsDatabaseRepository(application: Application) {

    private var catsDao: CatsDao

    private val catsDatabase = CatsDatabase.getInstance(application)

    init {
        catsDao = catsDatabase.catsDao()
    }

    suspend fun insert(cat: CatDatabaseEntity){
        catsDao.insertCat(cat)
    }

    suspend fun delete(cat: CatDatabaseEntity){
        catsDao.deleteCat(cat)
    }

    suspend fun update(cat: CatDatabaseEntity){
        catsDao.updateCat(cat)
    }

    suspend fun getAllCats():List<CatDatabaseEntity>{
        return catsDao.getAllCats()
    }

    suspend fun deleteAllCats(){
        catsDao.deleteAllCats()
    }

}