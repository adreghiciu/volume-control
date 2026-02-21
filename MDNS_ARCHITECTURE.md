# mDNS Architecture & Data Flow

## System Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                         Local Network (mDNS/DNS-SD)               │
│                                                                    │
│  Service: _volumecontrol._tcp.local                               │
│  ├─ Instance: "Living Room iMac"      (macOS)                     │
│  ├─ Instance: "Bedroom TV"            (Google TV)                 │
│  └─ Instance: "Phone Kitchen"         (Future)                    │
└────────────────────────────────────────────────────────────────────┘
           ▲                                      ▲
      Register                                Discover
           │                                      │
    ┌──────┴──────┐                        ┌──────┴──────┐
    │             │                        │             │
┌───▼────┐   ┌────▼───┐              ┌────▼───────────────┐
│ macOS  │   │ Google │              │  Android Phone     │
│ Server │   │  TV    │              │  (Discovery App)   │
└────────┘   └────────┘              └────────────────────┘
```

## Detailed Component Architecture

### 1. macOS Registration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      AppDelegate                            │
│                                                             │
│  applicationDidFinishLaunching()                           │
│    ├─ Create VolumeController                              │
│    ├─ Create HTTPServer(volumeController)                  │
│    └─ httpServer.start()  ◄─── Calls registerBonjourService
│                                                             │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────┐
│                   HTTPServer (MODIFIED)                     │
│                                                             │
│ Properties:                                                 │
│   - listener: NWListener (existing)                         │
│   - bonjourService: NWListener.Service (NEW)               │
│   - deviceFriendlyName: String (NEW)                       │
│                                                             │
│ Methods:                                                    │
│   - start() [modified]                                      │
│     ├─ listener = try NWListener()                          │
│     ├─ listener.start()                                     │
│     └─ registerBonjourService() ◄─── NEW                   │
│                                                             │
│   - registerBonjourService() [NEW]                          │
│     ├─ deviceFriendlyName = getDeviceName()                │
│     ├─ bonjourService = NWListener.Service(...)            │
│     └─ listener.service = bonjourService                   │
│                                                             │
│   - unregisterBonjourService() [NEW]                        │
│     └─ bonjourService = nil                                │
│                                                             │
│   - stop() [modified]                                       │
│     ├─ unregisterBonjourService() ◄─── NEW               │
│     └─ listener.cancel()                                   │
│                                                             │
│   - getDeviceName() -> String [NEW]                         │
│     ├─ Try: SCDynamicStoreCopyComputerName()               │
│     ├─ Fallback: Host.current().localizedName              │
│     └─ Return: "iMac" or "MacBook Pro"                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
        mDNS Network
    (Broadcast Service)
```

**Key Points**:
- Service Name: Device computer name (e.g., "iMac-Living Room")
- Service Type: `_volumecontrol._tcp`
- Domain: `local`
- Port: 8888 (from NWListener)

---

### 2. Google TV Registration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    BootReceiver                             │
│                                                             │
│  onReceive(context, intent)                                │
│    └─ startService(Intent(context, VolumeService::class)) │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────┐
│                  VolumeService (MODIFIED)                   │
│                                                             │
│ Properties:                                                 │
│   - volumeController: VolumeController (existing)           │
│   - httpServer: HttpServer (existing)                       │
│   - nsdManager: NsdManager (NEW)                           │
│   - serviceRegistered: Boolean (NEW)                        │
│   - serviceName: String (NEW)                              │
│                                                             │
│ Methods:                                                    │
│   - onCreate()                                              │
│     └─ nsdManager = getSystemService(NSD_SERVICE)          │
│                                                             │
│   - onStartCommand()                                        │
│     ├─ startForeground(notification)                        │
│     ├─ httpServer.start()                                   │
│     └─ registerMdnsService() ◄─── NEW                      │
│                                                             │
│   - registerMdnsService() [NEW]                             │
│     ├─ deviceName = Build.MODEL (or Build.DEVICE)          │
│     ├─ Create NsdServiceInfo:                               │
│     │  ├─ serviceName = "VolumeControl_${deviceName}"      │
│     │  ├─ serviceType = "_volumecontrol._tcp."             │
│     │  ├─ port = 8888                                       │
│     │  └─ host = InetAddress.getLocalHost()                │
│     ├─ nsdManager.registerService(serviceInfo, listener)   │
│     ├─ serviceRegistered = true                            │
│     └─ Log success/failure                                  │
│                                                             │
│   - unregisterMdnsService() [NEW]                           │
│     ├─ if (serviceRegistered)                              │
│     │  └─ nsdManager.unregisterService(registrationListener)│
│     └─ serviceRegistered = false                           │
│                                                             │
│   - onDestroy()                                             │
│     ├─ unregisterMdnsService() ◄─── NEW                   │
│     ├─ httpServer.stop()                                    │
│     └─ super.onDestroy()                                    │
│                                                             │
│   - RegistrationListener (inner class) [NEW]                │
│     ├─ onServiceRegistered(serviceInfo)                    │
│     │  └─ Log: "Service registered as: ${serviceInfo.serviceName}" │
│     ├─ onRegistrationFailed(serviceInfo, errorCode)        │
│     │  └─ Log error, retry logic (optional)                │
│     ├─ onServiceUnregistered(serviceInfo)                  │
│     │  └─ Log unregistered                                  │
│     └─ onUnregistrationFailed(serviceInfo, errorCode)      │
│        └─ Log error                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
              │
              ▼
        mDNS Network
    (Broadcast Service)
```

**Key Points**:
- Service Name: `VolumeControl_${Build.MODEL}` (e.g., "VolumeControl_Chromecast-with-Google-TV")
- Service Type: `_volumecontrol._tcp.` (note: trailing dot, no `.local`)
- Port: 8888
- Host: Local device IP address

---

### 3. Android Phone Discovery Flow

```
┌──────────────────────────────────────────────────────────────┐
│              DeviceListScreen (MODIFIED)                     │
│                                                              │
│  TopAppBar.actions {                                         │
│    IconButton(onClick = { showMenu = true })                 │
│  }                                                            │
│                                                              │
│  DropdownMenu {                                              │
│    DropdownMenuItem("Discover Devices") {                    │
│      ├─ showMenu = false                                     │
│      └─ Navigate to DiscoveryScreen() ◄─── NEW             │
│    }                                                          │
│  }                                                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────────────────┐
│              DiscoveryScreen (NEW)                           │
│                                                              │
│ ┌─────────────────────────────────────┐                     │
│ │  State:                             │                     │
│ │  - uiState.isDiscovering: Boolean   │                     │
│ │  - uiState.discoveredDevices: List  │                     │
│ │  - uiState.discoveryError: String?  │                     │
│ │                                     │                     │
│ │  LaunchedEffect(SCREEN_SHOW) {      │                     │
│ │    viewModel.startDiscovery()       │                     │
│ │  }                                  │                     │
│ │  DisposableEffect(SCREEN_HIDE) {    │                     │
│ │    viewModel.stopDiscovery()        │                     │
│ │  }                                  │                     │
│ └─────────────────────────────────────┘                     │
│                                                              │
│ Layout:                                                      │
│   ├─ TopAppBar: "Discover Devices"                          │
│   ├─ If (isDiscovering)                                     │
│   │  └─ CircularProgressIndicator("Scanning...")            │
│   ├─ LazyColumn {                                            │
│   │  items(discoveredDevices) { device ->                   │
│   │    Card {                                                │
│   │      Row {                                               │
│   │        Column {                                          │
│   │          Text(device.serviceName)                       │
│   │          Text("${device.ipAddress}:${device.port}")     │
│   │        }                                                  │
│   │        Button("Add") {                                   │
│   │          viewModel.addDiscoveredDevice(device)           │
│   │          popBackStack() ◄─── Close dialog              │
│   │        }                                                  │
│   │      }                                                    │
│   │    }                                                      │
│   │  }                                                        │
│   │ }                                                         │
│   ├─ If (discoveryError != null)                            │
│   │  └─ Text(discoveryError, color=Red)                     │
│   ├─ Divider()                                               │
│   ├─ Text("Manual Entry (Fallback)")                        │
│   ├─ TextField(label="IP Address")                          │
│   ├─ TextField(label="Port", value="8888")                  │
│   └─ Button("Add by IP") { viewModel.addDevice(ip, port) }  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────────────────┐
│              MainViewModel (MODIFIED)                        │
│                                                              │
│ Properties:                                                  │
│   - discoveryService: DiscoveryService (NEW)                 │
│   - _uiState: MutableStateFlow<MainUiState> (existing)       │
│                                                              │
│ New State Fields (in MainUiState):                           │
│   - discoveredDevices: List<DiscoveredDevice>               │
│   - isDiscovering: Boolean                                   │
│   - discoveryError: String?                                  │
│                                                              │
│ Methods:                                                     │
│   - startDiscovery() [NEW]                                   │
│     ├─ _uiState.isDiscovering = true                        │
│     ├─ discoveryService.startDiscovery()                    │
│     └─ Collect discoveredDevices from service               │
│                                                              │
│   - stopDiscovery() [NEW]                                    │
│     ├─ discoveryService.stopDiscovery()                     │
│     └─ _uiState.isDiscovering = false                       │
│                                                              │
│   - addDiscoveredDevice(device: DiscoveredDevice) [NEW]      │
│     ├─ Create Device:                                        │
│     │  ├─ name = device.serviceName                         │
│     │  ├─ host = device.host (or ipAddress)                │
│     │  └─ port = device.port                                │
│     ├─ repository.addDevice(device)                         │
│     └─ Update _uiState.devices                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────────────────┐
│              DiscoveryService (NEW)                          │
│                                                              │
│ Properties:                                                  │
│   - nsdManager: NsdManager                                   │
│   - _discoveredDevices: MutableStateFlow<List>              │
│   - discoveryListener: NsdManager.DiscoveryListener         │
│   - resolveListener: NsdManager.ResolveListener             │
│                                                              │
│ Public Methods:                                              │
│   - startDiscovery()                                         │
│     └─ nsdManager.discoverServices(                         │
│           "_volumecontrol._tcp.",                           │
│           NsdManager.PROTOCOL_DNS_SD,                       │
│           discoveryListener                                  │
│        )                                                     │
│                                                              │
│   - stopDiscovery()                                          │
│     ├─ nsdManager.stopServiceDiscovery(discoveryListener)   │
│     └─ _discoveredDevices.value = emptyList()              │
│                                                              │
│   - discoveredDevices: Flow<List<DiscoveredDevice>>         │
│     └─ return _discoveredDevices.asStateFlow()              │
│                                                              │
│ Discovery Listener Callbacks:                                │
│   - onStartDiscoveryFailed()                                │
│     └─ Log error, emit error to UI                          │
│                                                              │
│   - onStopDiscoveryFailed()                                  │
│     └─ Log error                                             │
│                                                              │
│   - onServiceFound(serviceInfo)                             │
│     ├─ Log found service                                    │
│     └─ nsdManager.resolveService(serviceInfo, listener)     │
│                                                              │
│   - onServiceLost(serviceInfo)                              │
│     └─ Remove from _discoveredDevices.value                │
│                                                              │
│ Resolve Listener Callbacks:                                  │
│   - onResolveFailed()                                        │
│     └─ Log error                                             │
│                                                              │
│   - onServiceResolved(serviceInfo)                          │
│     ├─ Extract:                                              │
│     │  ├─ serviceName = serviceInfo.serviceName              │
│     │  ├─ ipAddress = serviceInfo.host.hostAddress         │
│     │  ├─ port = serviceInfo.port                           │
│     │  └─ host = serviceInfo.host.hostName                  │
│     ├─ Create DiscoveredDevice                              │
│     └─ Add to _discoveredDevices                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
              │
              ▼
        mDNS Network
    (Query Services)
        │
        ├─ "Living Room iMac" (macOS)
        ├─ "Bedroom TV" (Google TV)
        └─ ...
```

---

## Data Models

### DiscoveredDevice (NEW)
```kotlin
package com.volumecontrol.android.model

data class DiscoveredDevice(
    val serviceName: String,      // e.g., "Living Room iMac"
    val serviceType: String,      // "_volumecontrol._tcp."
    val ipAddress: String,        // e.g., "192.168.1.100"
    val port: Int,                // 8888
    val host: String              // FQDN or hostname
)
```

### MainUiState (MODIFIED)
```kotlin
data class MainUiState(
    // Existing fields
    val devices: List<DeviceState> = emptyList(),
    val showAddEditDialog: Boolean = false,
    val editingDevice: Device? = null,

    // NEW fields for discovery
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val isDiscovering: Boolean = false,
    val discoveryError: String? = null
)
```

---

## Key Implementation Details

### 1. Service Registration Timing

**macOS**:
```
start() {
  listener = NWListener()
  listener.stateUpdateHandler = { state in
    if state == .ready {
      registerBonjourService()  // After listener is ready!
    }
  }
  listener.start()
}
```

**Google TV**:
```
onStartCommand() {
  startForeground(notification)
  httpServer.start()
  // After HTTP server starts, register mDNS
  registerMdnsService()
}
```

### 2. Service Discovery Timing

**Android**:
```
DiscoveryScreen {
  LaunchedEffect(true) {
    viewModel.startDiscovery()  // When screen appears
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.stopDiscovery()  // When screen disappears
    }
  }
}
```

### 3. Error Handling Strategy

- **Registration Errors**: Log but don't block server startup
- **Discovery Errors**: Show in UI, allow manual fallback
- **Resolution Errors**: Skip that service, continue discovering
- **Network Changes**: Let OS handle, gracefully handle exceptions

### 4. Device Name Strategy

| Platform | Method | Fallback |
|----------|--------|----------|
| macOS | `SCDynamicStoreCopyComputerName()` | `Host.current().localizedName` |
| Google TV | `Build.MODEL` | `Build.DEVICE` |
| Android | Manual entry | Manual entry |

---

## Testing Flows

### Test 1: macOS Registration
```
1. Start macOS app
2. Terminal: dns-sd -B _volumecontrol._tcp local
3. Expected: See macOS service instance
4. Verify: Service name matches computer name
5. Verify: Port 8888 shown
```

### Test 2: Google TV Registration + Discovery
```
1. Build/install Google TV app
2. Terminal: dns-sd -B _volumecontrol._tcp local
3. Expected: See Google TV service instance
4. Verify: Service name contains device model
5. Android: Open app → Menu → Discover Devices
6. Expected: Google TV appears in discovered list
```

### Test 3: Full End-to-End
```
1. Start macOS server
2. Start Google TV app
3. Open Android phone app
4. Click: Menu → Discover Devices
5. Expected: Both macOS and Google TV appear
6. Click: Add (macOS)
7. Expected: Returns to main screen, device added
8. Verify: Can control macOS volume from Android
9. Click: Menu → Discover Devices again
10. Click: Add (Google TV)
11. Expected: Can control both devices
```

---

## Lifecycle Diagram

```
┌─────────────────────────────────────┐
│         macOS App Started            │
└─────────────────────────────────────┘
            │
            ▼
    HTTPServer.start()
            │
            ▼
    registerBonjourService()
            │
            ▼
    ┌─────────────────────────────────────┐
    │   mDNS Service Registered           │
    │   _volumecontrol._tcp.local         │
    │   Instance: "iMac-Living Room"      │
    └─────────────────────────────────────┘
            │
            ▼
    ┌─────────────────────────────────────┐
    │  Android Phone Opens Discovery      │
    └─────────────────────────────────────┘
            │
            ▼
    DiscoveryService.startDiscovery()
            │
            ▼
    NsdManager.discoverServices()
            │
            ▼
    onServiceFound() → onServiceResolved()
            │
            ▼
    ┌─────────────────────────────────────┐
    │   DiscoveredDevice(                 │
    │     serviceName="iMac-Living Room", │
    │     ipAddress="192.168.1.100",      │
    │     port=8888                       │
    │   )                                 │
    └─────────────────────────────────────┘
            │
            ▼
    Display in DiscoveryScreen
            │
            ▼
    User clicks "Add"
            │
            ▼
    viewModel.addDiscoveredDevice()
            │
            ▼
    repository.addDevice(new Device(
      name="iMac-Living Room",
      host="192.168.1.100",
      port=8888
    ))
            │
            ▼
    Device persisted to DataStore
            │
            ▼
    Shown in main device list
            │
            ▼
    Can be controlled like normal device
```

---

## Sequence Diagram: Discovery & Addition

```
Android Phone        DiscoveryScreen      MainViewModel      DiscoveryService      NsdManager
    │                      │                     │                  │                   │
    │─ Click Discover ──>  │                     │                  │                   │
    │                      │─ startDiscovery ──> │                  │                   │
    │                      │                     │─ start discovery ─────────────────> │
    │                      │                     │                  │                   │
    │                      │                     │<─ onServiceFound("iMac") ──────────│
    │                      │                     │                  │ (resolve) ──────> │
    │                      │                     │<─ onServiceResolved ──────────────│
    │                      │                     │ DiscoveredDevice:                   │
    │                      │                     │ - serviceName: "iMac-Living Room"  │
    │                      │                     │ - ipAddress: "192.168.1.100"       │
    │                      │                     │ - port: 8888                       │
    │                      │                     │                  │                   │
    │<─ Show device ──────────────────────────── │                  │                   │
    │                      │                     │                  │                   │
    │─ Click Add ──────────────────────────────> │                  │                   │
    │                      │                     │─ stopDiscovery ──> │                 │
    │                      │                     │                  │─ stop ─────────> │
    │                      │                     │                  │                   │
    │                      │                     │─ addDiscoveredDevice ──────────┐    │
    │                      │                     │     (create Device)            │    │
    │                      │                     │ ─ repository.addDevice()       │    │
    │                      │                     │     (persist)                  │    │
    │                      │                     │<──────────────────────────────┘    │
    │<─ Close modal ──────────────────────────────────────────────────────────────────│
    │                      │                     │                  │                   │
    │─ Device in list ─────────────────────────────────────────────────────────────────│
    │                      │                     │                  │                   │
```

This comprehensive architecture provides the foundation for understanding how each component interacts and coordinates the mDNS discovery process across all platforms.
