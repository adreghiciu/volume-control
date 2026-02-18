package com.volumecontrol.googletv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Foreground service that manages the HTTP server lifecycle.
 * Auto-restarts if killed (START_STICKY).
 */
class VolumeService : Service() {
    private val binder = LocalBinder()
    private lateinit var volumeController: VolumeController
    private lateinit var httpServer: HttpServer
    private var isServerRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): VolumeService = this@VolumeService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VolumeService created")
        volumeController = VolumeController(this)
        httpServer = HttpServer(volumeController)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VolumeService started")

        // Create notification for foreground service
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start HTTP server if not already running
        if (!isServerRunning) {
            httpServer.start()
            isServerRunning = true
            Log.d(TAG, "HTTP Server started")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "VolumeService destroyed")
        httpServer.stop()
        isServerRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun isServerRunning(): Boolean = isServerRunning

    fun getVolumeController(): VolumeController = volumeController

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Control",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "HTTP server for TV volume control"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Control")
            .setContentText("HTTP server running on port 8888")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        private const val TAG = "VolumeService"
        private const val CHANNEL_ID = "volume_control_channel"
        private const val NOTIFICATION_ID = 1
    }
}
