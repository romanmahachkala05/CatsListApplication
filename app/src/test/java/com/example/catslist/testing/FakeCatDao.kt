package com.example.catslist.testing

import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.CatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory stand-in for the Room DAO. [insertCat] replaces a row with the same
 * id rather than duplicating it, matching the primary-key constraint on
 * `favoriteCatsTable`.
 */
class FakeCatDao : CatDao {

    private val rows = MutableStateFlow<List<CatEntity>>(emptyList())

    override fun getAllCats(): Flow<List<CatEntity>> = rows.asStateFlow()

    override suspend fun insertCat(cat: CatEntity) {
        rows.value = rows.value.filterNot { it.id == cat.id } + cat
    }

    override suspend fun deleteCat(cat: CatEntity) {
        rows.value = rows.value.filterNot { it.id == cat.id }
    }

    override suspend fun isFavorite(id: String): Boolean = rows.value.any { it.id == id }

    override suspend fun deleteAllCats() {
        rows.value = emptyList()
    }
}
