package com.example.catslist.data.local

import com.example.catslist.data.remote.CatDto
import com.example.catslist.domain.model.Cat

fun CatDto.toFeedEntity(sortOrder: Int): FeedCatEntity = FeedCatEntity(
    id = id,
    url = url,
    width = width,
    height = height,
    sortOrder = sortOrder,
)

/**
 * `isFavorite` is deliberately not set here — see the doc on
 * `CatRepositoryImpl.feed` for why favorite status is never baked into the paged
 * data itself.
 */
fun FeedCatEntity.toDomain(): Cat = Cat(
    id = id,
    url = url,
    width = width,
    height = height,
)
