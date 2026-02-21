# mDNS Implementation - Quick Reference

## What We're Building
Register mDNS services on macOS & Google TV so Android phones can discover them automatically.

**Service Type**: `_volumecontrol._tcp.local` on port 8888

---

## Quick Summary by Platform

### macOS (HTTPServer.swift)
- Use: Network.framework's NWListener.Service
- When: Register after listener.start(), unregister in stop()
- Device Name: Use `SCDynamicStoreCopyComputerName()` or hostname
- Code Path: `/macos/Sources/HTTPServer.swift` - Add 2 new methods

### Google TV (VolumeService.kt)
- Use: Android NsdManager.registerService()
- When: Register in onStartCommand(), unregister in onDestroy()
- Device Name: Use Build.MODEL or Build.DEVICE
- Code Path: `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt`
- Permissions: Add CHANGE_NETWORK_STATE to AndroidManifest.xml

### Android Phone (Discovery)
- Use: NsdManager.discoverServices() for `_volumecontrol._tcp.`
- Create: New DiscoveryService class to manage lifecycle
- Create: DiscoveryScreen UI with list of discovered devices
- Update: MainViewModel to expose discovered devices
- Update: DeviceListScreen menu to add "Discover Devices" option
- Permissions: Add CHANGE_NETWORK_STATE to AndroidManifest.xml

---

## Implementation Order

1. **macOS First** - Register Bonjour service in HTTPServer
2. **Google TV Second** - Register mDNS service in VolumeService
3. **Android Last** - Build discovery UI and ViewModel logic (depends on 1 & 2)

---

## Key Code Patterns

### macOS Registration (Swift)
```swift
// Create and register service
var bonjourService: NWListener.Service?

func registerBonjourService() throws {
    bonjourService = NWListener.Service(
        name: deviceFriendlyName,
        type: "_volumecontrol._tcp",
        domain: "local",
        port: port
    )
    // listener.newConnectionHandler includes bonjourService
}

func unregisterBonjourService() {
    bonjourService = nil
}
```

### Google TV Registration (Kotlin)
```kotlin
val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

val serviceInfo = NsdServiceInfo().apply {
    serviceName = "VolumeControl_${Build.MODEL}"
    serviceType = "_volumecontrol._tcp."
    port = 8888
    setHost(InetAddress.getLocalHost())
}

nsdManager.registerService(serviceInfo, object : NsdManager.RegistrationListener {
    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) { }
    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { }
})
```

### Android Discovery (Kotlin)
```kotlin
val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

nsdManager.discoverServices(
    "_volumecontrol._tcp.",
    NsdManager.PROTOCOL_DNS_SD,
    discoveryListener
)

// discoveryListener implements NsdManager.DiscoveryListener:
// - onServiceFound() → call nsdManager.resolveService()
// - onServiceResolved() → get InetAddress and port from ServiceInfo
// - Convert to DiscoveredDevice data class
```

---

## Files Overview

### Files to Create (Android Phone)
| File | Purpose |
|------|---------|
| `/android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt` | Manages NsdManager discovery lifecycle |
| `/android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt` | Data class for discovered services |
| `/android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt` | UI for device discovery |

### Files to Modify

| File | What to Add |
|------|------------|
| `/macos/Sources/HTTPServer.swift` | Register/unregister Bonjour service |
| `/googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt` | Register/unregister mDNS service |
| `/googletv/app/src/main/AndroidManifest.xml` | Add CHANGE_NETWORK_STATE permission |
| `/android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt` | Add discovery state management |
| `/android/app/src/main/java/com/volumecontrol/android/ui/DeviceListScreen.kt` | Add "Discover Devices" menu option |
| `/android/app/src/main/java/com/volumecontrol/android/MainActivity.kt` | Pass DiscoveryService to ViewModel |
| `/android/app/src/main/AndroidManifest.xml` | Add CHANGE_NETWORK_STATE permission |

---

## Testing Checklist

### macOS
- [ ] Run install script, start app
- [ ] Run `dns-sd -B _volumecontrol._tcp local` in Terminal
- [ ] Verify service appears with device name
- [ ] Close app, verify service unregistered

### Google TV
- [ ] Build and install app
- [ ] Check logcat: `adb logcat | grep NsdManager`
- [ ] Run `dns-sd -B _volumecontrol._tcp local` from Mac on same network
- [ ] Verify Google TV service appears

### Android Phone
- [ ] Build and install app
- [ ] Open app → Menu → "Discover Devices"
- [ ] Verify macOS service appears in list
- [ ] Verify Google TV service appears (if available)
- [ ] Click device to add
- [ ] Return to main screen, verify device in list
- [ ] Test volume control on discovered device
- [ ] Fallback: Try manual IP entry for comparison

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Local Network mDNS                        │
└─────────────────────────────────────────────────────────────┘
          ▲                              ▲
          │                              │
       (Register)                    (Register)
          │                              │
          │                              │
    ┌─────────────┐               ┌──────────────┐
    │  macOS App  │               │  Google TV   │
    │ HTTPServer  │               │ VolumeService│
    │ (Port 8888) │               │ (Port 8888)  │
    └─────────────┘               └──────────────┘


                    ┌─────────────────┐
                    │  Android Phone  │
                    │  (Discovery UI) │
                    └─────────────────┘
                          ▲
                          │
                      (Discover)
                          │
                    ┌─────────────────┐
                    │ DiscoveryService│
                    │  (NsdManager)   │
                    └─────────────────┘
```

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Service not discoverable | Ensure device on same WiFi network, no firewall blocking port 8888 |
| Registration fails silently | Check logs, add proper error logging to understand failure |
| Service appears but can't connect | Verify DNS-SD resolves to correct IP, test manual IP entry |
| Discovery finds nothing | Ensure registration working first, verify service type matches |
| Device name not friendly | Ensure device name logic works (fallback to hostname) |

---

## API Reference

### macOS (NWListener.Service)
```swift
NWListener.Service(
    name: String,           // Service instance name (e.g., "iMac-Living Room")
    type: "_volumecontrol._tcp",
    domain: "local",
    port: 8888
)
```

### Android (NsdManager)
```kotlin
// Registration
registerService(
    NsdServiceInfo().apply {
        serviceName = "VolumeControl_..."
        serviceType = "_volumecontrol._tcp."
        port = 8888
    },
    PROTOCOL_DNS_SD,
    registrationListener
)

// Discovery
discoverServices("_volumecontrol._tcp.", PROTOCOL_DNS_SD, discoveryListener)
resolveService(serviceInfo, discoveryListener)
```

---

## Next Steps

1. Review full MDNS_IMPLEMENTATION_PLAN.md for detailed requirements
2. Implement macOS registration in HTTPServer.swift
3. Test with `dns-sd -B _volumecontrol._tcp local`
4. Implement Google TV registration
5. Build Android discovery UI and integration
6. End-to-end testing across all platforms
