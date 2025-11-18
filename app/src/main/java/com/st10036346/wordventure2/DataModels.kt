package com.st10036346.wordventure2

// --- API Request/Response Models ---
data class CheckWordResponse(val valid: Boolean)

// --- Login Models ---
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val userId: String? = null, val message: String? = null)

// --- FCM Token Models ---

// Request body used to send the FCM token to the backend server.
data class FcmTokenRequest(
    val fcmToken: String
)