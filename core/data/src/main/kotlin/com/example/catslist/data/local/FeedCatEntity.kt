package com.example.catslist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One page's worth of the feed, cached so [CatFeedDao.pagingSource] has a local source
 * [androidx.paging.RemoteMediator] can trigger loads against. [sortOrder] is fetch order,
 * not `id` — the API hands back cats in no order a `SELECT` could recover on its own.
 */
@Entity(tableName = "feedCatsTable")
data class FeedCatEntity(
    @PrimaryKey val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val sortOrder: Int,
)
