package com.example.catslist.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.catslist.models.Cat

@Entity(
    tableName = "favoriteCats"
)
data class CatDatabaseEntity(
    @PrimaryKey  val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val favourite: Boolean
) {
    fun toCat(): Cat = Cat(
        id = id,
        url = url,
        width = width,
        height = height,
        favourite = favourite
    )

}

