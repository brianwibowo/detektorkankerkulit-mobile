package com.dicoding.asclepius.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CancerInfoApiService {
    @GET("top-headlines")
    fun getCancerArticles(
        @Query("q") query: String = "cancer",
        @Query("category") category: String = "health",
        @Query("language") language: String = "en",
        @Query("apiKey") apiKey: String = "5342367d93f3455c8b781f385bdfb7db"
    ): Call<NewsResponse>
}