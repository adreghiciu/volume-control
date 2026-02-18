package com.volumecontrol.googletv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that starts VolumeService when TV boots.
 * Handles both standard BOOT_COMPLETED and some OEM quick boot events.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot event received: ${intent.action}")
                val serviceIntent = Intent(context, VolumeService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
