package com.st10036346.wordventure2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

class WordVentureMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM_Service"
    private val CHANNEL_ID = "word_of_the_day_channel"
    private val PREFS_NAME = "AuthPrefs"
    private val KEY_USER_ID = "userId" // use this key to store the user's ID

    // Retrofit service instance
    private val apiService by lazy { RetrofitClient.instance }

    /**
     * Called if the FCM registration token is updated. This happens if the
     * app is reinstalled or the user clears app data.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Retrieve the stored user ID from local storage to associate the token.
        val userId = getLoggedInUserId()

        if (userId != null) {
            // Initiate the synchronisation process
            sendRegistrationToServer(userId, token)
        } else {
            // If no user ID is found, the token will be sent when the user logs in next.
            Log.w(TAG, "No logged-in user ID found locally to sync new token. Will sync on next successful login.")
        }
    }

    /**
     * Called when a message is received (e.g., when the daily word is ready).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // The default body/title will be used if provided by the server
            sendNotification(it.title ?: "WordVenture", it.body ?: "A new word is ready!")
        }
    }

    /**
     * Create and show a simple notification
     */
    private fun sendNotification(title: String, messageBody: String) {
        // When user taps the notification, open the Daily1 activity
        val intent = Intent(this, Daily1::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for Android O (8.0) and beyond
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Word Notifications", // User-visible name
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

    // Helper Functions for Token Sync

    private fun getLoggedInUserId(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * Makes the actual network call to Render backend to register the token.
     */
    private fun sendRegistrationToServer(userId: String, token: String) {
        // Run network operation in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestBody = FcmTokenRequest(token)
                val response = apiService.patchFcmToken(userId, requestBody).awaitResponse()

                if (response.isSuccessful) {
                    Log.i(TAG, "FCM Token successfully registered for user $userId. Status: ${response.code()}")
                } else {
                    // Log the failure, but don't stop the user from using the app
                    Log.e(TAG, "FCM Token registration FAILED for user $userId. Status: ${response.code()}, Body: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM Token registration network error for $userId: ${e.message}")
            }
        }
    }
}
