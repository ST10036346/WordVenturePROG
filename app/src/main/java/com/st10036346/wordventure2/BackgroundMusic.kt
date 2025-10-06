package com.st10036346.wordventure2

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class BackgroundMusicService : Service() {

    companion object {
        private var player: MediaPlayer? = null


        fun setVolume(isMuted: Boolean) {
            val volume = if (isMuted) 0.0f else 1.0f
            player?.setVolume(volume, volume)
        }


        fun isPlaying(): Boolean {
            return player?.isPlaying ?: false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        if (player == null) {
            player = MediaPlayer.create(this, com.st10036346.wordventure2.R.raw.mysterious)
            player?.isLooping = true
            // Initial volume is set to 1.0f (unmuted)
            player?.setVolume(1.0f, 1.0f)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (player != null && player?.isPlaying == false) {
            player?.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
        player = null
    }
}