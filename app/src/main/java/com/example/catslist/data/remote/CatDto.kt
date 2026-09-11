package com.example.catslist.data.remote

import com.google.gson.annotations.SerializedName

/** Wire model for TheCatAPI's `/v1/images/search` response. No `favorite` field — the API doesn't know about it. */
data class CatDto(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
)
