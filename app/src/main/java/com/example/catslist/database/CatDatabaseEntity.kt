package com.example.catslist.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.catslist.models.Cat

@Entity(
    tableName = "favoriteCatsTable"
)
data class CatDatabaseEntity(
    @PrimaryKey  val id: String,
    val url: String,
    val width: Int,
    val height: Int,
    val favorite: Boolean
) {
    fun toCat(): Cat = Cat(
        id = id,
        url = url,
        width = width,
        height = height,
        favorite = favorite
    )

    companion object {

        fun fromCat(cat: Cat): CatDatabaseEntity = CatDatabaseEntity(
            id = cat.id,
            url = cat.url,
            width = cat.width,
            height = cat.height,
            favorite = cat.favorite
        )

    }
}

