package com.example.catslist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * An abstract class rather than an interface so [toggleFavorite] can carry a body
 * that Room wraps in a real transaction — see the comment on it.
 */
@Dao
abstract class CatDao {

    @Insert
    abstract suspend fun insertCat(cat: CatEntity)

    @Delete
    abstract suspend fun deleteCat(cat: CatEntity)

    @Query("SELECT * FROM favoriteCatsTable")
    abstract fun getAllCats(): Flow<List<CatEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoriteCatsTable WHERE id = :id)")
    abstract suspend fun isFavorite(id: String): Boolean

    @Query("DELETE FROM favoriteCatsTable")
    abstract suspend fun deleteAllCats()

    /**
     * Adds or removes [cat] in one transaction.
     *
     * The read and the write have to be atomic: two quick taps on the same cat each
     * launch their own coroutine, and split across two statements both could read
     * "not a favorite" before either writes. That would insert twice — which
     * [insertCat] aborts on, since `id` is the primary key and the default conflict
     * strategy is `ABORT` — and would leave the cat favorited after two taps that
     * should have cancelled each other out. Room runs an `@Transaction` method on
     * its transaction thread, so concurrent calls queue instead of interleaving.
     */
    @Transaction
    open suspend fun toggleFavorite(cat: CatEntity) {
        if (isFavorite(cat.id)) deleteCat(cat) else insertCat(cat)
    }
}
