package com.example.catslist.testing

import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.CatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory stand-in for the Room DAO. Extends the real [CatDao] rather than
 * reimplementing it, so `toggleFavorite` under test is the very body Room runs —
 * a fake that reimplemented it could quietly drift from the real one.
 *
 * What it can't reproduce is the `@Transaction` guarantee: this runs the block
 * inline, so it proves the toggle's logic, not its atomicity. That needs a real
 * database, i.e. an instrumented or Robolectric test.
 */
class FakeCatDao : CatDao() {

    private val rows = MutableStateFlow<List<CatEntity>>(emptyList())

    override fun getAllCats(): Flow<List<CatEntity>> = rows.asStateFlow()

    /**
     * Rejects a duplicate id, as the real `@Insert` does: `id` is the primary key
     * and the default conflict strategy is `ABORT`. Room would raise
     * `SQLiteConstraintException`, which doesn't exist off-device.
     */
    override suspend fun insertCat(cat: CatEntity) {
        check(rows.value.none { it.id == cat.id }) { "UNIQUE constraint failed: favoriteCatsTable.id (${cat.id})" }
        rows.value = rows.value + cat
    }

    override suspend fun deleteCat(cat: CatEntity) {
        rows.value = rows.value.filterNot { it.id == cat.id }
    }

    override suspend fun isFavorite(id: String): Boolean = rows.value.any { it.id == id }

    override suspend fun deleteAllCats() {
        rows.value = emptyList()
    }
}
