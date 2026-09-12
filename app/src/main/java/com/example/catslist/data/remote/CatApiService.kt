package com.example.catslist.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface CatApiService {

    @GET("v1/images/search")
    suspend fun requestCatInfo(@Query("limit") limit: Int): List<CatDto>
}
