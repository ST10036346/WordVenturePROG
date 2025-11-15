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

class WordVentureMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM_Service"
    private val CHANNEL_ID = "word_of_the_day_channel"
    private val PREFS_NAME = "AuthPrefs"
    private val KEY_USER_ID = "userId" // We will use this key to store the user's ID

    /**
     * Called if the FCM registration token is updated. This happens if the
     * app is reinstalled or the user clears app data.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // This is where we need to re-sync the token with your backend API.
        // We retrieve the stored user ID from local storage to associate the token.
        val userId = getLoggedInUserId()

        if (userId != null) {
            // Note: We use a coroutine to make the network call asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                sendRegistrationToServer(userId, token)
            }
        } else {
            // If we can't find a user ID, the token will be sent when the user logs in next.
            Log.w(TAG, "No logged-in user ID found locally to sync new token.")
        }
    }

    /**
     * Called when a message is received (e.g., when the daily word is ready).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if the message contains a data payload (usually for custom content)
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // You can process the data here, e.g., show a more specific notification
            // based on the word ID or category sent by the server.
        }

        // Check if the message contains a notification payload (for display)
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

    // --- Helper Functions for Token Sync ---

    private fun getLoggedInUserId(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }

    private fun sendRegistrationToServer(userId: String, token: String) {
        // You need to define a function in ApiService.kt to handle this API call
        // E.g., @POST("users/{userId}/fcm-token") fun updateFCMToken(@Path("userId") userId: String, @Body tokenData: TokenRequest): Call<Void>
        Log.i(TAG, "Attempting to sync token for user $userId: $token")
        // TODO: Implement actual Retrofit call here using your ApiService.kt
    }
}