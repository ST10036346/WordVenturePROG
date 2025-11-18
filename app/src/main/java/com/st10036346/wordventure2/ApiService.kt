package com.st10036346.wordventure2

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Header

interface ApiService {
    @GET("random-word")
    fun getRandomWord(): Call<WordResponse>

    @POST("check-word")
    fun checkWord(@Body guess: WordGuess): Call<CheckWordResponse>

     // endpoint to register or update the FCM token for a specific user.
     // Maps to: PATCH /api/users/{userId}/fcm-token
     @PATCH("api/users/{userId}/fcm-token")
    fun patchFcmToken(
        @Path("userId") userId: String,
        @Body request: FcmTokenRequest,
        // @Header("Authorization") authToken: String
    ): Call<Unit>
}