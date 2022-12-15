package com.example.catslist.tools

import com.example.catslist.models.Cat
import retrofit2.Call
import retrofit2.http.GET

interface CatRequest {

    @GET("v1/images/search")
    fun requestCatInfo(): Call<List<Cat>>

}