package com.st10036346.wordventure2

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("random-word")
    fun getRandomWord(): Call<WordResponse>

    // In ApiService.kt
    @POST("check-word") // The URL no longer needs the placeholder
    fun checkWord(@Body guess: WordGuess): Call<CheckWordResponse>


}
data class CheckWordResponse(val valid: Boolean)
