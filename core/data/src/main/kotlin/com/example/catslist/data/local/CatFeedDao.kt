package com.example.catslist.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * An abstract class rather than an interface so [refresh] and [append] can carry bodies
 * Room wraps in a real transaction — the same reason [CatDao] is one, see its own doc.
 */
@Dao
abstract class CatFeedDao {

    /**
     * Deliberately not joined against `favoriteCatsTable` — see the doc on
     * [com.example.catslist.data.repository.CatRepositoryImpl.feed] for why a favorite
     * toggle must not invalidate this query.
     */
    @Query("SELECT * FROM feedCatsTable ORDER BY sortOrder ASC")
    abstract fun pagingSource(): PagingSource<Int, FeedCatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(cats: List<FeedCatEntity>)

    @Query("DELETE FROM feedCatsTable")
    abstract suspend fun clearFeed()

    /** Where the next append picks up — one past the highest [FeedCatEntity.sortOrder] cached. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM feedCatsTable")
    abstract suspend fun nextSortOrder(): Int

    @Query("SELECT * FROM feedRemoteKeysTable WHERE id = ${FeedRemoteKeysEntity.SINGLETON_ID}")
    abstract suspend fun getRemoteKeys(): FeedRemoteKeysEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setRemoteKeys(keys: FeedRemoteKeysEntity)

    /**
     * Replaces the whole cached feed and its remote key in one transaction, so a concurrent
     * read of [pagingSource] never lands between the clear and the refill.
     */
    @Transaction
    open suspend fun refresh(cats: List<FeedCatEntity>, nextPage: Int?) {
        clearFeed()
        insertAll(cats)
        setRemoteKeys(FeedRemoteKeysEntity(nextPage = nextPage))
    }

    /** Appends one page and advances the remote key, atomically. */
    @Transaction
    open suspend fun append(cats: List<FeedCatEntity>, nextPage: Int?) {
        insertAll(cats)
        setRemoteKeys(FeedRemoteKeysEntity(nextPage = nextPage))
    }
}
