package com.volumecontrol.googletv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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
    private lateinit var nsdManager: NsdManager
    private var nsdServiceRegistration: NsdManager.RegistrationListener? = null

    inner class LocalBinder : Binder() {
        fun getService(): VolumeService = this@VolumeService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VolumeService created")
        volumeController = VolumeController(this)
        httpServer = HttpServer(volumeController)
        nsdManager = getSystemService(NsdManager::class.java)
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

            // Register mDNS service
            registerMdnsService()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "VolumeService destroyed")
        httpServer.stop()
        isServerRunning = false

        // Unregister mDNS service
        unregisterMdnsService()

        super.onDestroy()
    }

    private fun registerMdnsService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = android.os.Build.MODEL  // Use device model as service name
            serviceType = "_volumecontrol._tcp"
            port = 8888
        }

        nsdServiceRegistration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(p0: NsdServiceInfo?) {
                Log.d(TAG, "mDNS service registered: ${serviceInfo.serviceName}._volumecontrol._tcp.local")
            }

            override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {
                Log.e(TAG, "Failed to register mDNS service: $p1")
            }

            override fun onServiceUnregistered(p0: NsdServiceInfo?) {
                Log.d(TAG, "mDNS service unregistered")
            }

            override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {
                Log.e(TAG, "Failed to unregister mDNS service: $p1")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdServiceRegistration!!)
    }

    private fun unregisterMdnsService() {
        nsdServiceRegistration?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering mDNS service: ${e.message}")
            }
        }
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
