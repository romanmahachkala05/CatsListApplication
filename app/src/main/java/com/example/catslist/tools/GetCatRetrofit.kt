package com.example.catslist.tools

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GetCatRetrofit {

    const val baseUrl = "https://api.thecatapi.com"
    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getService() = retrofit.create(CatRequestApiService::class.java)
}