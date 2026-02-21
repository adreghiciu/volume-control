# mDNS Code Templates & Implementation Stubs

This document provides code skeleton templates for implementing mDNS across all platforms.

---

## 1. macOS HTTPServer.swift - Bonjour Registration

**File**: `/macos/Sources/HTTPServer.swift`

**Add these imports**:
```swift
import Foundation
import Network
import SystemConfiguration  // For computer name
```

**Add these properties to HTTPServer class**:
```swift
class HTTPServer {
    let port: NWEndpoint.Port = 8888
    var listener: NWListener?
    var volumeController: VolumeController

    // NEW PROPERTIES:
    var bonjourService: NWListener.Service?

    init(volumeController: VolumeController) {
        self.volumeController = volumeController
    }

    // ... existing code ...
}
```

**Modify start() method**:
```swift
func start() throws {
    listener = try NWListener(using: .tcp, on: port)

    listener?.newConnectionHandler = { [weak self] connection in
        self?.handleConnection(connection)
    }

    listener?.stateUpdateHandler = { [weak self] state in
        switch state {
        case .ready:
            print("HTTP server listening on port 8888")
            // REGISTER BONJOUR SERVICE AFTER LISTENER IS READY
            do {
                try self?.registerBonjourService()
            } catch {
                print("Failed to register Bonjour service: \(error)")
                // Non-fatal error - HTTP server still works
            }
        case .failed(let error):
            print("HTTP server error: \(error)")
        default:
            break
        }
    }

    listener?.start(queue: DispatchQueue(label: "http-server-queue"))
}
```

**Modify stop() method**:
```swift
func stop() {
    unregisterBonjourService()  // ADD THIS LINE
    listener?.cancel()
}
```

**Add these new methods**:
```swift
// Get device-friendly name for Bonjour registration
private func getDeviceName() -> String {
    // Method 1: Try to get computer name from System Configuration
    let computerName = SCDynamicStoreCopyComputerName(nil, nil) as String?
    if let name = computerName, !name.isEmpty {
        return name
    }

    // Fallback: Use hostname
    if let hostname = Host.current().localizedName, !hostname.isEmpty {
        return hostname
    }

    // Final fallback
    return "Mac-VolumeControl"
}

// Register service with Bonjour (mDNS)
private func registerBonjourService() throws {
    let deviceName = getDeviceName()

    bonjourService = NWListener.Service(
        name: deviceName,
        type: "_volumecontrol._tcp",
        domain: "local",
        port: port
    )

    guard let service = bonjourService else {
        throw NSError(domain: "HTTPServer", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to create Bonjour service"])
    }

    // Associate service with listener
    // Note: The listener automatically advertises this service
    listener?.service = service

    print("Bonjour service registered: \(deviceName)._volumecontrol._tcp.local")
}

// Unregister service
private func unregisterBonjourService() {
    if bonjourService != nil {
        listener?.service = nil
        bonjourService = nil
        print("Bonjour service unregistered")
    }
}
```

**Dependencies to Add**:
- SystemConfiguration framework (for SCDynamicStoreCopyComputerName)
  - In Xcode: Target → Build Phases → Link Binary With Libraries → Add SystemConfiguration

---

## 2. Google TV VolumeService.kt - NsdManager Registration

**File**: `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt`

**Add imports**:
```kotlin
package com.volumecontrol.googletv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager           // NEW
import android.net.nsd.NsdServiceInfo       // NEW
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.net.InetAddress                 // NEW
```

**Add properties to VolumeService class**:
```kotlin
class VolumeService : Service() {
    private val binder = LocalBinder()
    private lateinit var volumeController: VolumeController
    private lateinit var httpServer: HttpServer
    private var isServerRunning = false

    // NEW PROPERTIES:
    private lateinit var nsdManager: NsdManager
    private var serviceRegistered = false
    private var currentServiceInfo: NsdServiceInfo? = null

    // ... existing code ...
}
```

**Modify onCreate() method**:
```kotlin
override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "VolumeService created")
    volumeController = VolumeController(this)
    httpServer = HttpServer(volumeController)

    // ADD THIS:
    nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
}
```

**Modify onStartCommand() method**:
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "VolumeService started")

    createNotificationChannel()
    val notification = createNotification()
    startForeground(NOTIFICATION_ID, notification)

    if (!isServerRunning) {
        httpServer.start()
        isServerRunning = true
        Log.d(TAG, "HTTP Server started")

        // ADD THIS: Register mDNS service after HTTP server starts
        registerMdnsService()
    }

    return START_STICKY
}
```

**Modify onDestroy() method**:
```kotlin
override fun onDestroy() {
    Log.d(TAG, "VolumeService destroyed")

    // ADD THIS: Unregister before stopping server
    unregisterMdnsService()

    httpServer.stop()
    isServerRunning = false
    super.onDestroy()
}
```

**Add these new methods**:
```kotlin
// Register mDNS service
private fun registerMdnsService() {
    if (serviceRegistered) {
        Log.w(TAG, "Service already registered, skipping")
        return
    }

    try {
        // Build device name
        val deviceModel = Build.MODEL.replace(" ", "-")
        val serviceName = "VolumeControl_$deviceModel"

        // Create service info
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = "_volumecontrol._tcp."
            this.port = 8888
            setHost(InetAddress.getLocalHost())
        }

        // Register with listener
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "mDNS Service registered: ${serviceInfo.serviceName}")
                    currentServiceInfo = serviceInfo
                    serviceRegistered = true
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS Service registration failed: $errorCode")
                    serviceRegistered = false
                    // Note: HTTP server still running even if registration fails
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "mDNS Service unregistered")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "mDNS Service unregistration failed: $errorCode")
                }
            }
        )

        Log.d(TAG, "mDNS Service registration initiated: $serviceName")
    } catch (e: Exception) {
        Log.e(TAG, "Error registering mDNS service", e)
        // Non-fatal: HTTP server continues without mDNS
    }
}

// Unregister mDNS service
private fun unregisterMdnsService() {
    if (!serviceRegistered || currentServiceInfo == null) {
        return
    }

    try {
        // Create a simple unregistration listener
        val unregListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "mDNS Service unregistered: ${serviceInfo.serviceName}")
                serviceRegistered = false
                currentServiceInfo = null
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "mDNS Service unregistration failed: $errorCode")
            }
        }

        nsdManager.unregisterService(unregListener)
        Log.d(TAG, "mDNS Service unregistration initiated")
    } catch (e: Exception) {
        Log.e(TAG, "Error unregistering mDNS service", e)
    }
}

companion object {
    private const val TAG = "VolumeService"
    private const val CHANNEL_ID = "volume_control_channel"
    private const val NOTIFICATION_ID = 1
}
```

**Update AndroidManifest.xml**:
```xml
<!-- Add this permission (if not already present) -->
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

---

## 3. Android Phone Discovery - DiscoveryService.kt

**File**: `/android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt` (CREATE NEW)

```kotlin
package com.volumecontrol.android.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.volumecontrol.android.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiscoveryService(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolvedServices = mutableMapOf<String, NsdServiceInfo>()

    fun startDiscovery() {
        Log.d(TAG, "Starting mDNS discovery")

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                // Resolve to get IP and port
                nsdManager.resolveService(serviceInfo, createResolveListener())
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                _discoveredDevices.value = _discoveredDevices.value.filter {
                    it.serviceName != serviceInfo.serviceName
                }
            }
        }

        nsdManager.discoverServices(
            "_volumecontrol._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stopDiscovery() {
        Log.d(TAG, "Stopping mDNS discovery")
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
            discoveryListener = null
        }
        _discoveredDevices.value = emptyList()
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolution failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")

                try {
                    val ipAddress = serviceInfo.host?.hostAddress ?: "unknown"
                    val port = serviceInfo.port
                    val hostName = serviceInfo.host?.hostName ?: serviceInfo.serviceName

                    val discoveredDevice = DiscoveredDevice(
                        serviceName = serviceInfo.serviceName,
                        serviceType = "_volumecontrol._tcp.",
                        ipAddress = ipAddress,
                        port = port,
                        host = hostName
                    )

                    // Add or update in list
                    val current = _discoveredDevices.value.toMutableList()
                    val existingIndex = current.indexOfFirst { it.serviceName == serviceInfo.serviceName }

                    if (existingIndex >= 0) {
                        current[existingIndex] = discoveredDevice
                    } else {
                        current.add(discoveredDevice)
                    }

                    _discoveredDevices.value = current
                    Log.d(TAG, "Added device: ${discoveredDevice.serviceName} @ $ipAddress:$port")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing resolved service", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "DiscoveryService"
    }
}
```

---

## 4. Android Phone Discovery - DiscoveredDevice.kt

**File**: `/android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt` (CREATE NEW)

```kotlin
package com.volumecontrol.android.model

data class DiscoveredDevice(
    val serviceName: String,
    val serviceType: String,
    val ipAddress: String,
    val port: Int,
    val host: String
) {
    // Helper for display
    val displayName: String
        get() = serviceName

    val displayAddress: String
        get() = "$ipAddress:$port"
}
```

---

## 5. Android Phone UI - MainViewModel.kt (Modifications)

**File**: `/android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt` (MODIFY)

**Add to imports**:
```kotlin
import com.volumecontrol.android.data.DiscoveryService
import com.volumecontrol.android.model.DiscoveredDevice
import kotlinx.coroutines.launch
```

**Modify MainUiState**:
```kotlin
data class MainUiState(
    val devices: List<DeviceState> = emptyList(),
    val showAddEditDialog: Boolean = false,
    val editingDevice: Device? = null,

    // NEW FIELDS:
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val isDiscovering: Boolean = false,
    val discoveryError: String? = null,
    val showDiscoveryDialog: Boolean = false
)
```

**Add to MainViewModel init or constructor**:
```kotlin
class MainViewModel(
    private val repository: DeviceRepository,
    private val apiClient: VolumeApiClient,
    private val discoveryService: DiscoveryService  // NEW
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDevices()

        // NEW: Collect discovered devices
        viewModelScope.launch {
            discoveryService.discoveredDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(
                    discoveredDevices = devices
                )
            }
        }
    }

    // ... existing code ...
}
```

**Add these new methods**:
```kotlin
// Open discovery dialog
fun showDiscoveryDialog() {
    _uiState.value = _uiState.value.copy(
        showDiscoveryDialog = true,
        isDiscovering = false,
        discoveredDevices = emptyList(),
        discoveryError = null
    )
}

// Close discovery dialog
fun closeDiscoveryDialog() {
    stopDiscovery()
    _uiState.value = _uiState.value.copy(
        showDiscoveryDialog = false
    )
}

// Start mDNS discovery
fun startDiscovery() {
    _uiState.value = _uiState.value.copy(isDiscovering = true)
    viewModelScope.launch {
        try {
            discoveryService.startDiscovery()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isDiscovering = false,
                discoveryError = e.message
            )
        }
    }
}

// Stop mDNS discovery
fun stopDiscovery() {
    discoveryService.stopDiscovery()
    _uiState.value = _uiState.value.copy(isDiscovering = false)
}

// Add discovered device to device list
fun addDiscoveredDevice(discovered: DiscoveredDevice) {
    viewModelScope.launch {
        val device = Device(
            name = discovered.serviceName,
            host = discovered.ipAddress,
            port = discovered.port
        )
        repository.addDevice(device)
        _uiState.value = _uiState.value.copy(
            devices = _uiState.value.devices + DeviceState(device = device),
            showDiscoveryDialog = false
        )
        // Fetch volume for newly added device
        fetchAllVolumes()
    }
}
```

**Update MainViewModelFactory**:
```kotlin
class MainViewModelFactory(
    private val repository: DeviceRepository,
    private val apiClient: VolumeApiClient,
    private val discoveryService: DiscoveryService  // NEW
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, apiClient, discoveryService) as T
    }
}
```

---

## 6. Android Phone UI - DiscoveryScreen.kt

**File**: `/android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt` (CREATE NEW)

```kotlin
package com.volumecontrol.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.volumecontrol.android.model.DiscoveredDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    uiState: MainUiState,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onAddDevice: (DiscoveredDevice) -> Unit,
    onDismiss: () -> Unit
) {
    // Start discovery when screen appears
    LaunchedEffect(Unit) {
        onStartDiscovery()
    }

    // Stop discovery when screen disappears
    DisposableEffect(Unit) {
        onDispose {
            onStopDiscovery()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Discover Devices") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Scanning indicator
            if (uiState.isDiscovering) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning for devices...")
                }
            }

            // Error message
            if (uiState.discoveryError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Error: ${uiState.discoveryError}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Discovered devices list
            Text(
                text = "Available Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(uiState.discoveredDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = device.displayAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onAddDevice(device) }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            if (uiState.discoveredDevices.isEmpty() && !uiState.isDiscovering) {
                Text(
                    text = "No devices found. Make sure they are on the same network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Manual entry section (fallback)
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Or Add Manually",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var manualIp by remember { mutableStateOf("") }
            var manualPort by remember { mutableStateOf("8888") }
            var manualName by remember { mutableStateOf("") }

            TextField(
                value = manualName,
                onValueChange = { manualName = it },
                label = { Text("Device Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            TextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("IP Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            TextField(
                value = manualPort,
                onValueChange = { manualPort = it },
                label = { Text("Port") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (manualName.isNotEmpty() && manualIp.isNotEmpty()) {
                        val device = DiscoveredDevice(
                            serviceName = manualName,
                            serviceType = "_volumecontrol._tcp.",
                            ipAddress = manualIp,
                            port = manualPort.toIntOrNull() ?: 8888,
                            host = manualIp
                        )
                        onAddDevice(device)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Device")
            }
        }
    }
}
```

---

## 7. Android Phone UI - DeviceListScreen.kt (Modifications)

**File**: `/android/app/src/main/java/com/volumecontrol/android/ui/DeviceListScreen.kt` (MODIFY)

**Update function signature**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    uiState: MainUiState,
    onAddDevice: () -> Unit,
    onDiscoverDevices: () -> Unit,  // NEW
    onEditDevice: (Device) -> Unit,
    // ... rest of parameters ...
) {
    // ... existing code ...
}
```

**Update DropdownMenu in TopAppBar**:
```kotlin
DropdownMenu(
    expanded = showMenu,
    onDismissRequest = { showMenu = false }
) {
    // NEW: Add discover devices option
    DropdownMenuItem(
        text = { Text("Discover Devices") },
        onClick = {
            showMenu = false
            onDiscoverDevices()
        }
    )

    DropdownMenuItem(
        text = { Text(stringResource(R.string.retry_all)) },
        onClick = {
            showMenu = false
            onRetryAll()
        }
    )

    // ... rest of menu items ...
}
```

---

## 8. Android Phone MainActivity.kt (Modifications)

**File**: `/android/app/src/main/java/com/volumecontrol/android/MainActivity.kt` (MODIFY)

**Add imports**:
```kotlin
import com.volumecontrol.android.data.DiscoveryService  // NEW
```

**Modify onCreate**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val repository = DeviceRepository(applicationContext)
    val apiClient = VolumeApiClient()
    val discoveryService = DiscoveryService(applicationContext)  // NEW

    setContent {
        VolumeControlTheme {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository, apiClient, discoveryService)  // MODIFIED
            )
            viewModelInstance = viewModel

            val uiState by viewModel.uiState.collectAsState()

            DeviceListScreen(
                uiState = uiState,
                onAddDevice = { viewModel.showAddDeviceDialog() },
                onDiscoverDevices = { viewModel.showDiscoveryDialog() },  // NEW
                onEditDevice = { device -> viewModel.showEditDeviceDialog(device) },
                // ... rest of callbacks ...
            )

            // Existing AddEditDeviceDialog
            if (uiState.showAddEditDialog) {
                // ... existing code ...
            }

            // NEW: Discovery Dialog
            if (uiState.showDiscoveryDialog) {
                Dialog(
                    onDismissRequest = { viewModel.closeDiscoveryDialog() },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DiscoveryScreen(
                            uiState = uiState,
                            onStartDiscovery = { viewModel.startDiscovery() },
                            onStopDiscovery = { viewModel.stopDiscovery() },
                            onAddDevice = { device -> viewModel.addDiscoveredDevice(device) },
                            onDismiss = { viewModel.closeDiscoveryDialog() }
                        )
                    }
                }
            }
        }
    }
}
```

---

## 9. AndroidManifest.xml Updates

**Files**:
- `/android/app/src/main/AndroidManifest.xml` (MODIFY)
- `/googletv/app/src/main/AndroidManifest.xml` (MODIFY)

**Add this permission**:
```xml
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

---

## Summary of Changes

| File | Type | Changes |
|------|------|---------|
| `/macos/Sources/HTTPServer.swift` | Modify | Add Bonjour registration/unregistration |
| `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt` | Modify | Add NsdManager registration |
| `/googletv/app/src/main/AndroidManifest.xml` | Modify | Add CHANGE_NETWORK_STATE permission |
| `/android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt` | Create | Discovery service using NsdManager |
| `/android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt` | Create | Data class for discovered devices |
| `/android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt` | Create | Discovery UI |
| `/android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt` | Modify | Add discovery state and methods |
| `/android/app/src/main/java/com/volumecontrol/android/ui/DeviceListScreen.kt` | Modify | Add "Discover Devices" menu option |
| `/android/app/src/main/java/com/volumecontrol/android/MainActivity.kt` | Modify | Integrate DiscoveryService |
| `/android/app/src/main/AndroidManifest.xml` | Modify | Add CHANGE_NETWORK_STATE permission |

---

## Next Steps

1. Copy code templates into respective files
2. Adjust package names and imports as needed
3. Build and test each platform in order:
   - macOS first
   - Google TV second
   - Android phone last
4. Verify mDNS registration with `dns-sd -B _volumecontrol._tcp local`
5. Test end-to-end discovery and device control
