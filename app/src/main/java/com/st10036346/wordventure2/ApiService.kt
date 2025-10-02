package com.st10036346.wordventure2

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("random-word")
    fun getRandomWord(): Call<WordResponse>
}