package com.volumecontrol.googletv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import java.net.NetworkInterface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var volumeService: VolumeService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            volumeService = binder.getService()
            isBound = true
            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Start the service
        startForegroundService(Intent(this, VolumeService::class.java))

        setContent {
            TvScreen(
                context = this,
                onStartServer = { startServer() },
                onStopServer = { stopServer() },
                getVolumeController = { volumeService?.getVolumeController() },
                isServiceRunning = { isBound && volumeService?.isServerRunning() == true }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, VolumeService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun startServer() {
        if (!isBound) {
            startForegroundService(Intent(this, VolumeService::class.java))
        }
    }

    private fun stopServer() {
        if (isBound) {
            volumeService?.stopSelf()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

class MainViewModel : ViewModel() {
    var volume by mutableIntStateOf(0)
    var ipAddress by mutableStateOf("Getting IP...")
    var isServiceRunning by mutableStateOf(false)
    var volumeController: VolumeController? by mutableStateOf(null)

    fun updateVolume() {
        volumeController?.let {
            volume = it.getVolume()
        }
    }

    fun updateServiceStatus(isRunning: Boolean) {
        isServiceRunning = isRunning
    }
}

@Composable
fun TvScreen(
    context: Context,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    getVolumeController: () -> VolumeController?,
    isServiceRunning: () -> Boolean
) {
    val viewModel: MainViewModel = viewModel()

    // Initialize volume controller
    LaunchedEffect(Unit) {
        val controller = getVolumeController()
        if (controller != null) {
            viewModel.volumeController = controller
            viewModel.updateVolume()
        }
    }

    // Poll volume and service status every second
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateVolume()
            viewModel.updateServiceStatus(isServiceRunning())
            delay(1000)
        }
    }

    // Get IP address on load
    LaunchedEffect(Unit) {
        viewModel.ipAddress = getLocalIpAddress(context) ?: "Not available"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            "Volume Control",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Volume display
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 36.dp)
                .background(Color(0xFF16213E), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                "Volume:",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                "${viewModel.volume}%",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE94560),
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // IP Address display
        Text(
            "IP: ${viewModel.ipAddress}:8888",
            fontSize = 36.sp,
            color = Color(0xFFB0B0B0),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Service status
        Text(
            if (viewModel.isServiceRunning) "Server: Running" else "Server: Not Running",
            fontSize = 36.sp,
            color = if (viewModel.isServiceRunning) Color(0xFF4CAF50) else Color(0xFFE94560),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Button(
                onClick = { onStartServer() },
                modifier = Modifier
                    .width(300.dp)
                    .height(80.dp)
            ) {
                Text("Start Server", fontSize = 28.sp)
            }

            Button(
                onClick = { onStopServer() },
                modifier = Modifier
                    .width(300.dp)
                    .height(80.dp)
            ) {
                Text("Stop Server", fontSize = 28.sp)
            }
        }
    }
}

fun getLocalIpAddress(context: Context): String? {
    return try {
        // Try WiFi first
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null && connectionInfo.ipAddress != 0) {
                val ipInt = connectionInfo.ipAddress
                return String.format(
                    "%d.%d.%d.%d",
                    (ipInt and 0xff),
                    ((ipInt shr 8) and 0xff),
                    ((ipInt shr 16) and 0xff),
                    ((ipInt shr 24) and 0xff)
                )
            }
        }

        // Fallback to NetworkInterface
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
            if (!networkInterface.isLoopback && networkInterface.isUp) {
                networkInterface.inetAddresses?.toList()?.forEach { address ->
                    if (!address.isLoopbackAddress && address.hostAddress != null) {
                        val hostAddress = address.hostAddress
                        if (hostAddress.contains(".") && !hostAddress.contains(":")) {
                            return hostAddress
                        }
                    }
                }
            }
        }
        null
    } catch (e: Exception) {
        Log.e("IP_ADDRESS", "Error getting IP address", e)
        null
    }
}
