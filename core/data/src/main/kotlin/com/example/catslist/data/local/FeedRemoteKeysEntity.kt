package com.example.catslist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single row recording the next page [CatFeedRemoteMediator][com.example.catslist.data.repository.CatFeedRemoteMediator]
 * should ask the API for. The feed only ever appends, so one global key is enough — a
 * per-item remote-keys table (the usual Paging 3 pattern) exists to support prepending,
 * which this feed never does.
 */
@Entity(tableName = "feedRemoteKeysTable")
data class FeedRemoteKeysEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val nextPage: Int?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
