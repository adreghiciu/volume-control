package com.volumecontrol.googletv

import android.content.Context
import android.media.AudioManager

/**
 * Wrapper around AudioManager for volume control.
 * Normalizes between 0-100 API range and 0-maxVolume device range.
 * Uses rounding to minimize drift in normalization.
 */
class VolumeController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /**
     * Get current volume as percentage (0-100)
     */
    @Synchronized
    fun getVolume(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) {
            ((currentVolume.toFloat() / maxVolume) * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Set volume from percentage (0-100)
     */
    @Synchronized
    fun setVolume(volumePercent: Int) {
        val clipped = volumePercent.coerceIn(0, 100)
        val deviceVolume = ((clipped.toFloat() / 100) * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, deviceVolume, 0)
    }

    /**
     * Get the maximum volume steps on this device
     */
    fun getMaxVolume(): Int = maxVolume

    /**
     * Get muted status
     */
    @Synchronized
    fun isMuted(): Boolean {
        return audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
    }

    /**
     * Set muted status
     */
    @Synchronized
    fun setMuted(muted: Boolean) {
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, muted)
    }
}
