# mDNS Device Discovery Implementation Plan

## Overview
Implement multicast DNS (mDNS) device discovery across the Volume Control system to automatically discover services on the local network, eliminating the need for users to manually enter IP addresses.

**Service Definition**: `_volumecontrol._tcp.local` on port 8888

---

## Platform-Specific Implementation Details

### 1. macOS HTTPServer Enhancement

**Current State**:
- Location: `/macos/Sources/HTTPServer.swift`
- Uses Network framework's NWListener on port 8888
- Started in AppDelegate.applicationDidFinishLaunching()

**Changes Required**:

1. **Add Bonjour Service Registration**:
   - Import: `import Network`
   - Create `NWListener.Service` representing the mDNS service
   - Register service on HTTPServer startup
   - Unregister service on shutdown

2. **Implementation Details**:
   ```swift
   // New properties in HTTPServer class:
   - var bonjourService: NWListener.Service?
   - var deviceFriendlyName: String  // e.g., "Living Room iMac"

   // New methods:
   - func registerBonjourService() throws -> String  // Returns service name
   - func unregisterBonjourService()
   ```

3. **Device Name Strategy**:
   - Option 1: Use hostname (via `Host.current().localizedName`)
   - Option 2: Use computer name (via `SCDynamicStoreCopyComputerName()`)
   - Option 3: Combine with device type hint (e.g., "iMac-Living-Room")
   - **Recommendation**: Start with `SCDynamicStoreCopyComputerName()` + fallback to hostname

4. **Integration Points**:
   - Call `registerBonjourService()` after listener.start() succeeds
   - Call `unregisterBonjourService()` in HTTPServer.stop()
   - Add error handling for registration failures

5. **Framework Requirements**:
   - Network framework (already imported)
   - SystemConfiguration framework (for computer name)

**File to Modify**: `/home/adreghi/Work/volume-control/macos/Sources/HTTPServer.swift`

---

### 2. Google TV (Android) mDNS Registration

**Current State**:
- Location: `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt`
- Foreground service managing HTTP server lifecycle
- HTTP server running on port 8888

**Changes Required**:

1. **Add NsdManager for mDNS**:
   - Use Android's built-in NsdManager (Android API 4+)
   - Register service on service creation
   - Unregister service on service destruction

2. **Implementation Details**:
   ```kotlin
   // New properties in VolumeService class:
   - private lateinit var nsdManager: NsdManager
   - private var serviceRegistered = false
   - private val serviceName = "VolumeControl"  // or device-friendly name
   - private val serviceType = "_volumecontrol._tcp."

   // New methods:
   - private fun registerMdnsService()
   - private fun unregisterMdnsService()
   - private inner class MdnsRegistrationListener : NsdManager.RegistrationListener
   ```

3. **Device Name Strategy**:
   - Use device model name: `Build.MODEL` (e.g., "Chromecast-with-Google-TV")
   - Fallback to: `Build.DEVICE` or static "VolumeControl-TV"
   - Format: `"{device_model}_volumecontrol"` or similar

4. **Registration Flow**:
   ```
   onCreate() → Initialize NsdManager
   onStartCommand() → registerMdnsService() after HTTP server starts
   onDestroy() → unregisterMdnsService() before stopping HTTP server
   ```

5. **Manifest Changes**:
   - Add permission: `android.permission.CHANGE_NETWORK_STATE` (if needed)
   - Verify: `android.permission.INTERNET` (already present)
   - Verify: `android.permission.ACCESS_NETWORK_STATE` (already present)

6. **Error Handling**:
   - Log registration failures but don't block HTTP server startup
   - Track service registration state to prevent double-registration
   - Handle service name conflicts (NsdManager provides renamed name)

**Files to Modify**:
- `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt`
- `/googletv/app/src/main/AndroidManifest.xml`

---

### 3. Android Phone - mDNS Discovery UI & Logic

**Current State**:
- Location: `/android/app/src/main/java/com/volumecontrol/android/`
- Manual device addition via AddEditDeviceDialog
- Jetpack Compose UI with MVVM architecture

**Changes Required**:

#### A. Discovery Service/Logic

1. **Create DiscoveryService**:
   - Location: `/android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt`
   - Wraps NsdManager discovery listener
   - Provides Flow<List<DiscoveredDevice>> for Compose consumption
   - Handles service discovery lifecycle

2. **Create DiscoveredDevice Model**:
   - Location: `/android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt`
   ```kotlin
   data class DiscoveredDevice(
       val serviceName: String,        // "Living Room iMac"
       val serviceType: String,        // "_volumecontrol._tcp."
       val ipAddress: String,
       val port: Int,
       val host: String                // FQDN or IP
   )
   ```

3. **NsdManager Integration**:
   ```kotlin
   // DiscoveryService responsibilities:
   - Start discovery on init/when requested
   - Stop discovery on cleanup
   - Handle NsdManager.DiscoveryListener callbacks
   - Convert ServiceInfo to DiscoveredDevice
   - Expose discovered devices as StateFlow<List<DiscoveredDevice>>
   - Handle errors gracefully
   ```

#### B. ViewModel Enhancement

1. **Extend MainViewModel**:
   - Add `discoveredDevices: StateFlow<List<DiscoveredDevice>>`
   - Add `isDiscovering: StateFlow<Boolean>`
   - Add `discoveryError: StateFlow<String?>`
   - Add `startDiscovery()` function
   - Add `stopDiscovery()` function
   - Add `addDiscoveredDevice(device: DiscoveredDevice)` function

2. **Lifecycle Management**:
   - Start discovery when user navigates to discovery screen
   - Stop discovery when user leaves/closes discovery screen
   - Handle ViewModel scope cleanup

#### C. UI Components

1. **Create DiscoveryScreen**:
   - Location: `/android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt`
   - Features:
     - "Scanning..." progress indicator during discovery
     - List of discovered services
     - Each item shows: service name, IP, port
     - "Add" button for each device
     - Manual IP entry fallback field
     - Cancel/Done buttons

2. **Add Discovery Menu Item**:
   - Update DeviceListScreen TopAppBar menu
   - Add "Discover Devices" option
   - Navigate to DiscoveryScreen as modal/dialog
   - Option: Add FAB for quick discovery access

3. **Compose Considerations**:
   - Use LaunchedEffect to start/stop discovery based on screen visibility
   - Show loading spinner while discovering
   - Handle no-devices-found state
   - Show fallback manual IP entry option

#### D. Integration with Device Addition

1. **Two-Path Device Addition**:
   - **Path 1**: Discover → Select from list → Add (auto-fills IP/port)
   - **Path 2**: Manual → Enter IP + port → Add (current behavior)

2. **Dialog Flow**:
   - Main screen: Show "Discover Devices" menu option
   - Click: Opens modal with discovery UI
   - User clicks device: Confirms addition
   - Device added to configured list
   - Modal closes, main screen refreshes

#### E. Permissions

1. **Manifest Changes**:
   - Add: `android.permission.CHANGE_NETWORK_STATE` (required for NsdManager)
   - Verify: `android.permission.INTERNET` (already present)
   - Verify: `android.permission.ACCESS_NETWORK_STATE` (may be needed)

**Files to Create**:
- `/android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt`
- `/android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt`
- `/android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt`

**Files to Modify**:
- `/android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt`
- `/android/app/src/main/java/com/volumecontrol/android/ui/DeviceListScreen.kt`
- `/android/app/src/main/java/com/volumecontrol/android/MainActivity.kt`
- `/android/app/src/main/AndroidManifest.xml`

---

## Implementation Sequence

### Phase 1: macOS Registration (Foundation)
1. Modify HTTPServer.swift to register Bonjour service
2. Add device name resolution logic
3. Test with Bonjour browser on macOS
4. Verify unregistration on shutdown

### Phase 2: Google TV Registration
1. Modify VolumeService.kt to register mDNS service
2. Add NsdManager initialization
3. Add manifest permissions
4. Test with network discovery tools

### Phase 3: Android Discovery UI (Depends on Phase 1 & 2)
1. Create DiscoveryService.kt
2. Create DiscoveredDevice.kt model
3. Update MainViewModel with discovery state
4. Create DiscoveryScreen.kt UI
5. Integrate with DeviceListScreen navigation
6. Update AndroidManifest.xml
7. Test end-to-end discovery

---

## Key Technical Considerations

### macOS (Swift/Bonjour)
- **Framework**: Network.framework (available 10.15+)
- **Service Type**: `_volumecontrol._tcp.local`
- **Registration**: Automatic via NWListener.Service()
- **DNS-SD**: Handled by OS

### Android (Kotlin/NsdManager)
- **API Level**: API 4+ (available in all modern Android)
- **Service Type**: Must be `_volumecontrol._tcp.` (note: trailing dot, no `.local`)
- **Registration**: Explicit via NsdManager.registerService()
- **Discovery**: Explicit via NsdManager.discoverServices()

### Name Resolution
- **macOS**: Use `Host.getByName()` or just return hostname
- **Android Discovery**: Automatically resolves IP from ServiceInfo.getInetAddress()
- **Fallback**: Always provide manual IP entry as backup

### Error Handling
- **Registration Failures**: Log but don't block server startup
- **Discovery Failures**: Show error message, allow manual entry
- **Service Conflicts**: Use renamed service name from NsdManager
- **Network Changes**: Handle gracefully during discovery

---

## Testing Strategy

### macOS Testing
```bash
# Terminal 1: Start macOS app
./macos/install.sh

# Terminal 2: Browse services
dns-sd -B _volumecontrol._tcp local

# Should see: Instance Name: "iMac-Living Room" | Type: _volumecontrol._tcp | Domain: local
```

### Android Google TV Testing
```bash
# Verify NsdManager registration:
# adb logcat | grep "NsdManager"
# Use Android Network Analyzer app

# Verify from macOS:
dns-sd -B _volumecontrol._tcp local
# Should see: TV Instance Name (e.g., "Chromecast")
```

### Android Phone Testing
1. Ensure Google TV is on same network with mDNS service running
2. Open Android app → Menu → "Discover Devices"
3. Verify discovered TV appears in list
4. Click to add → Verify device added to main list
5. Test volume control on discovered device

---

## Benefits

1. **Zero-Configuration Discovery**: Users don't need to know IP addresses
2. **Automatic Service Advertisement**: Devices announce themselves automatically
3. **Network-Wide Awareness**: All devices discoverable on local network
4. **Fallback Option**: Manual IP entry always available as backup
5. **Cross-Platform**: Works across macOS, Google TV, and Android phones

---

## Migration Path

- **Current**: Manual IP entry (still works)
- **New**: Discover + manual entry (both options available)
- **Future**: Could default to discovery, keep manual entry as fallback
