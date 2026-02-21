# mDNS Device Discovery Implementation Guide

## Overview

This guide documents the complete implementation plan for adding multicast DNS (mDNS/Bonjour/DNS-SD) device discovery to the Volume Control system. Instead of manually entering IP addresses, users will be able to discover devices automatically on their local network.

## What is mDNS?

mDNS (multicast DNS) is a protocol that allows devices on a local network to discover services without a central DNS server. It works through:

1. **Service Registration**: A device (macOS/Google TV) announces its service on the local network
   - Service Type: `_volumecontrol._tcp.local`
   - Service Instance: Device-friendly name (e.g., "Living Room iMac")
   - Port: 8888
   - Transport: TCP

2. **Service Discovery**: Other devices (Android phone) search for and find registered services
   - Query: "Give me all `_volumecontrol._tcp` services"
   - Response: List of found services with IP addresses and ports
   - Resolution: Get detailed information including IP address

## Documentation Files

This implementation includes comprehensive documentation:

### 1. **MDNS_QUICK_REFERENCE.md**
   - Start here for a quick overview
   - Platform-by-platform summary
   - Implementation order
   - Key code patterns
   - Common issues and solutions

### 2. **MDNS_IMPLEMENTATION_PLAN.md**
   - Detailed requirements for each platform
   - File locations and specific changes needed
   - Integration points and lifecycle management
   - Testing strategies
   - Migration path

### 3. **MDNS_ARCHITECTURE.md**
   - System architecture diagrams
   - Detailed component flows for each platform
   - Data models and structures
   - Sequence diagrams
   - Lifecycle diagrams
   - Testing flows

### 4. **MDNS_CODE_TEMPLATES.md**
   - Ready-to-use code skeletons
   - Complete method implementations
   - Key code patterns
   - Import statements needed
   - File-by-file implementation guide

### 5. **MDNS_README.md** (this file)
   - Overview and getting started

## Implementation Timeline

### Phase 1: macOS Registration (Week 1)
**Goal**: Enable macOS app to advertise its service via Bonjour

1. Modify `HTTPServer.swift` to register Bonjour service
2. Add device name resolution logic
3. Test with `dns-sd` command
4. Verify service unregistration on shutdown

**Estimated Time**: 2-3 hours
**Files Modified**: 1 (HTTPServer.swift)

### Phase 2: Google TV Registration (Week 1-2)
**Goal**: Enable Google TV app to advertise its service via NsdManager

1. Modify `VolumeService.kt` to register mDNS service
2. Add NsdManager initialization
3. Update AndroidManifest.xml permissions
4. Test with `dns-sd` command from macOS

**Estimated Time**: 2-3 hours
**Files Modified**: 2 (VolumeService.kt, AndroidManifest.xml)

### Phase 3: Android Phone Discovery (Week 2-3)
**Goal**: Enable Android phone to discover and add devices automatically

1. Create `DiscoveryService.kt` for NsdManager wrapper
2. Create `DiscoveredDevice.kt` data model
3. Update `MainViewModel.kt` with discovery state
4. Create `DiscoveryScreen.kt` UI
5. Update `DeviceListScreen.kt` to add "Discover Devices" menu
6. Update `MainActivity.kt` to integrate DiscoveryService
7. Update AndroidManifest.xml permissions
8. End-to-end testing

**Estimated Time**: 4-5 hours
**Files Created**: 3 (DiscoveryService, DiscoveredDevice, DiscoveryScreen)
**Files Modified**: 4 (MainViewModel, DeviceListScreen, MainActivity, AndroidManifest.xml)

## Quick Start

### For Developers Starting Implementation:

1. **Read First**: Start with `MDNS_QUICK_REFERENCE.md` (5 min)
2. **Understand Architecture**: Review `MDNS_ARCHITECTURE.md` diagrams (10 min)
3. **Get Code Templates**: Use `MDNS_CODE_TEMPLATES.md` (ongoing reference)
4. **Reference Details**: Check `MDNS_IMPLEMENTATION_PLAN.md` for specifics
5. **Implement**: Follow Phase 1, 2, 3 in order

### For Code Review:

1. Review architecture in `MDNS_ARCHITECTURE.md`
2. Check implementation details in `MDNS_IMPLEMENTATION_PLAN.md`
3. Verify code follows templates in `MDNS_CODE_TEMPLATES.md`
4. Test according to testing strategies in `MDNS_IMPLEMENTATION_PLAN.md`

## Key Technologies

### macOS
- **Framework**: Network.framework + SystemConfiguration
- **API**: NWListener.Service
- **Feature**: Bonjour (Apple's mDNS implementation)
- **Available Since**: macOS 10.15 (our target)

### Google TV (Android)
- **Framework**: Android NDK - Network Service Discovery
- **API**: NsdManager
- **Service Type**: `_volumecontrol._tcp.` (note: trailing dot)
- **Available Since**: Android API 4+ (universally supported)

### Android Phone
- **Framework**: Android NDK - Network Service Discovery
- **API**: NsdManager for discovery, NsdManager for resolution
- **Service Type**: `_volumecontrol._tcp.`
- **Permission**: CHANGE_NETWORK_STATE

## Service Definition

**Service Type**: `_volumecontrol._tcp.local`

**Naming Convention**:
- **macOS**: Computer name (e.g., "Living Room iMac")
- **Google TV**: Device model (e.g., "VolumeControl_Chromecast-with-Google-TV")
- **Port**: 8888 (same for all)

## Testing Without Full Implementation

You can test individual platforms before others are complete:

```bash
# Terminal 1: Start macOS app
./macos/install.sh

# Terminal 2: List all mDNS services on network
dns-sd -B _tcp local
# Or specifically:
dns-sd -B _volumecontrol._tcp local

# Expected output:
# Browsing for _volumecontrol._tcp
#         Living Room iMac._volumecontrol._tcp.local. can be reached at iMac.local:8888
```

## Common Implementation Issues

### Issue: Service not registering
- **macOS**: Ensure listener is in `.ready` state before registering
- **Google TV**: Check that HTTP server starts before NsdManager registration
- **Solution**: Review lifecycle diagrams in `MDNS_ARCHITECTURE.md`

### Issue: Android can't discover services
- **Cause**: Different network or firewall blocking mDNS
- **Solution**: Ensure all devices on same Wi-Fi network
- **Debug**: Use Android Network Analyzer or Wireshark to see mDNS packets

### Issue: Service appears but can't connect
- **Cause**: DNS-SD resolution returning wrong IP
- **Solution**: Check DNS resolution, test manual IP entry
- **Debug**: Use `nslookup` or `dig` to verify DNS resolution

## Future Enhancements

1. **Service Quality**: Add service quality indicators (signal strength, latency)
2. **Service Filtering**: Filter by device type or location
3. **Service Caching**: Cache discovered services for offline use
4. **Persistent Aliases**: Remember "friendly names" for devices
5. **Auto-Reconnect**: Automatically reconnect to devices
6. **Service Monitoring**: Monitor service availability and health
7. **Cross-Platform**: Extend to iOS, Windows, Linux clients

## Performance Considerations

- **Discovery Time**: Typically 1-3 seconds on local network
- **Battery Impact**: Minimal when not actively discovering
- **Network Load**: Minimal - mDNS uses multicast, not unicast
- **CPU Impact**: Negligible background monitoring

## Security Considerations

- **Local Network Only**: mDNS services are only visible on local network
- **No Authentication**: mDNS itself provides no authentication
- **Trust Local Network**: Assumes trusted local network
- **Future**: Could add authentication in HTTP API layer

## Migration Path

### Current State (Before Implementation)
- Manual IP entry required
- No device discovery
- Users need to know exact IP addresses

### After Phase 1 & 2
- Devices register themselves
- Android can still use manual entry
- No discovery UI yet

### After Phase 3 (Complete)
- Full discovery UI
- Automatic device detection
- Manual entry as fallback
- Zero-configuration setup

## Rollback Strategy

If mDNS implementation causes issues:

1. **macOS**: Remove Bonjour registration, HTTP server still works
2. **Google TV**: Remove NsdManager registration, HTTP server still works
3. **Android**: Remove discovery UI, manual entry still works

All platforms gracefully degrade if mDNS fails - the HTTP server continues operating independently.

## Documentation Structure

```
Volume Control Project/
├── MDNS_README.md (this file)
│   └── Overview and getting started
├── MDNS_QUICK_REFERENCE.md
│   └── Quick summary by platform
├── MDNS_IMPLEMENTATION_PLAN.md
│   └── Detailed requirements and specifications
├── MDNS_ARCHITECTURE.md
│   └── System design and data flows
├── MDNS_CODE_TEMPLATES.md
│   └── Ready-to-use code skeletons
│
└── Implementation Files (will be created):
    ├── macos/Sources/HTTPServer.swift (modified)
    ├── googletv/app/src/main/java/com/volumecontrol/googletv/VolumeService.kt (modified)
    ├── googletv/app/src/main/AndroidManifest.xml (modified)
    ├── android/app/src/main/java/com/volumecontrol/android/data/DiscoveryService.kt (new)
    ├── android/app/src/main/java/com/volumecontrol/android/model/DiscoveredDevice.kt (new)
    ├── android/app/src/main/java/com/volumecontrol/android/ui/DiscoveryScreen.kt (new)
    ├── android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt (modified)
    ├── android/app/src/main/java/com/volumecontrol/android/ui/DeviceListScreen.kt (modified)
    ├── android/app/src/main/java/com/volumecontrol/android/MainActivity.kt (modified)
    └── android/app/src/main/AndroidManifest.xml (modified)
```

## Getting Help

If you encounter issues during implementation:

1. **Check Quick Reference**: `MDNS_QUICK_REFERENCE.md` - Common issues section
2. **Review Architecture**: `MDNS_ARCHITECTURE.md` - Understand component interactions
3. **Check Code Templates**: `MDNS_CODE_TEMPLATES.md` - Verify code matches pattern
4. **Read Implementation Plan**: `MDNS_IMPLEMENTATION_PLAN.md` - Review detailed requirements
5. **Test Manually**: Use `dns-sd` command to verify mDNS is working

## Success Criteria

### Phase 1 Complete (macOS Registration)
- [ ] macOS app starts without errors
- [ ] `dns-sd -B _volumecontrol._tcp local` shows macOS service
- [ ] Service name matches computer name
- [ ] Service disappears when app closes
- [ ] HTTP server still works if Bonjour registration fails

### Phase 2 Complete (Google TV Registration)
- [ ] Google TV app starts without errors
- [ ] `dns-sd -B _volumecontrol._tcp local` shows Google TV service
- [ ] macOS can see both services
- [ ] Service disappears when app closes
- [ ] HTTP server still works if mDNS registration fails

### Phase 3 Complete (Android Discovery)
- [ ] Android app builds without errors
- [ ] "Discover Devices" menu option appears
- [ ] Discovery screen shows both macOS and Google TV
- [ ] Clicking "Add" adds device to main list
- [ ] Manual IP entry works as fallback
- [ ] Volume control works on discovered devices
- [ ] No errors in logcat

## References

### Apple Bonjour Documentation
- [Bonjour Overview](https://developer.apple.com/bonjour/)
- [NSNetServiceBrowser (older API)](https://developer.apple.com/documentation/foundation/nsnetservicebrowser)
- Network.framework uses modern APIs automatically

### Android NsdManager Documentation
- [NsdManager Reference](https://developer.android.com/reference/android/net/nsd/NsdManager)
- [Network Service Discovery](https://developer.android.com/training/connect-devices-wirelessly/nsd)

### mDNS/DNS-SD Standards
- [RFC 6762 - mDNS](https://tools.ietf.org/html/rfc6762)
- [RFC 6763 - DNS-SD](https://tools.ietf.org/html/rfc6763)

## Contact & Support

For questions or issues with this implementation:
1. Review the relevant documentation file
2. Check the code templates for reference implementation
3. Verify architecture diagrams for component interactions
4. Test with native tools (`dns-sd`, `adb logcat`, etc.)

---

**Last Updated**: February 2025
**Status**: Ready for Implementation
**Complexity**: Medium (distributed across 3 platforms)
**Estimated Total Time**: 8-10 hours

---

## Next Steps

1. Read `MDNS_QUICK_REFERENCE.md` (5 minutes)
2. Review `MDNS_ARCHITECTURE.md` diagrams (10 minutes)
3. Start with Phase 1: macOS implementation
4. Follow `MDNS_CODE_TEMPLATES.md` for code patterns
5. Test each phase independently before moving to next

Good luck with the implementation!
