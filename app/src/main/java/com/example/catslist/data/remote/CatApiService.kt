package com.example.catslist.data.remote

import retrofit2.http.GET

interface CatApiService {

    @GET("v1/images/search")
    suspend fun requestCatInfo(): List<CatDto>
}
