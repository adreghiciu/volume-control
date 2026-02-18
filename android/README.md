# Volume Control Android App (Phase 2)

An Android app that remotely controls volume on macOS devices via HTTP API.

## Features

- **Multi-device management**: Add, edit, and delete devices (name, IP, port)
- **Persistent storage**: Devices saved between app launches via DataStore
- **Real-time volume control**: Sliders for each device with responsive updates
- **Parallel fetching**: Fetches volume from all devices simultaneously on app focus
- **Error handling**: Shows "unreachable" messages for offline devices
- **Non-blocking UI**: All network calls are asynchronous with coroutines
- **Network timeout**: 3-second timeout prevents hanging on unreachable devices
- **Retry mechanism**: "Retry All" option in menu to re-fetch all volumes

## Architecture

The app follows **MVVM + StateFlow** architecture:

- **Model** (`model/Device.kt`): Data classes for devices and their state
- **Data** (`data/`): Repository for persistence, API client for HTTP
- **UI** (`ui/`): ViewModel, Composable screens, theme
- **MainActivity.kt**: Entry point, triggers volume fetch on resume

## Tech Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| UI | Jetpack Compose + Material 3 | Modern, reactive, Android 14 native |
| HTTP | OkHttp | Simple GET/POST, no overhead |
| Persistence | DataStore + Gson | Lightweight, schema-less JSON storage |
| Concurrency | Coroutines + async | Non-blocking, parallel operations |
| Architecture | MVVM + StateFlow | Clean, testable, reactive |
| DI | Manual | App is small, no framework needed |

## Building

### Prerequisites
- **Docker** (installed and configured)
- No need for Android Studio or SDK (Docker handles everything)

### Build Commands

```bash
cd android

# Build APK using Docker
docker build -t volume-control-android-builder .

# Extract APK from container to android directory
docker run --rm -v $(pwd):/output volume-control-android-builder \
  cp /workspace/VolumeControl.apk /output/VolumeControl.apk
```

The generated APK will be at: `android/VolumeControl.apk` (24 MB debug build)

## Installation

### Install on Android Device

1. Connect Android 14 device via USB
2. Enable USB debugging on device
3. Install the APK:
   ```bash
   adb install android/VolumeControl.apk
   ```
4. App will be available in app drawer as "Volume Control"

Alternatively, copy `VolumeControl.apk` to device and install manually.

## Usage

### Add Device
1. Tap **+** button or **Add Device** from menu
2. Enter device name, IP address, and port (default 8888)
3. Tap **Save**

### Control Volume
- Drag slider to adjust volume
- Changes are sent immediately to the device

### Edit Device
1. Long-press device card
2. Select **Edit**
3. Modify details and save

### Delete Device
1. Long-press device card
2. Select **Delete**

### Retry All Volumes
- Tap **⋮** menu → **Retry All**
- Useful if devices were unreachable and now available

### App Focus
- When app comes to foreground, volumes are fetched from all devices
- No manual action needed; updates arrive as responses come in

## API Integration

The app communicates with the **Phase 1 macOS app** via HTTP:

- **GET `/volume`** → Returns `{"volume": 0-100}`
- **POST `/volume`** → Accepts `{"volume": <int>}`, sets volume

Example:
```bash
# From Android device (if macOS is at 192.168.1.100:8888)
adb shell curl http://192.168.1.100:8888/volume
# Returns: {"volume": 75}

adb shell curl -X POST http://192.168.1.100:8888/volume \
    -H "Content-Type: application/json" \
    -d '{"volume": 50}'
# Returns: {"volume": 50}
```

## Testing

### Setup
1. Run macOS app on local network (Phase 1)
2. Get Mac's IP: `ifconfig | grep "inet "`
3. Install Android app
4. Add device with Mac's IP

### Verify
1. Open app → add device → device appears
2. Drag slider → observe Mac volume change
3. Kill app → reopen → devices still present
4. Unplug Mac from network → see error in app
5. Plug back in → tap Retry All → slider updates

## File Structure

```
android/
├── Dockerfile                          # Docker build configuration
├── build.gradle.kts                    # Root build config
├── gradle.properties                   # Gradle JVM options
├── settings.gradle.kts                 # Gradle settings
├── VolumeControl.apk                   # Generated APK (after build)
├── app/
│   ├── build.gradle.kts                # App-level gradle config
│   ├── proguard-rules.pro              # Obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml         # App permissions + activity
│       └── java/com/volumecontrol/android/
│           ├── MainActivity.kt         # Entry point
│           ├── model/
│           │   └── Device.kt           # Data classes
│           ├── data/
│           │   ├── DeviceRepository.kt # DataStore persistence
│           │   └── VolumeApiClient.kt  # OkHttp HTTP client
│           └── ui/
│               ├── MainViewModel.kt    # StateFlow, business logic
│               ├── DeviceListScreen.kt # Main screen
│               ├── DeviceCard.kt       # Device UI component
│               ├── AddEditDeviceDialog.kt # Add/edit dialog
│               └── theme/
│                   └── Theme.kt        # Material 3 colors + typography
```

## Known Limitations

1. **No authentication**: Assumes local network is trusted (no API key needed)
2. **One control per device**: Only volume control, no other macOS features
3. **No offline queue**: Requests fail if device is unreachable (intentional, per plan)
4. **Manual IP entry**: No mDNS/Bonjour discovery (simplifies code, user enters IP)
5. **Android 14+ only**: Targets modern API to avoid legacy code

## Future Enhancements (Not in Phase 2)

- Bluetooth volume control
- Google TV support (Phase 3)
- Device discovery via mDNS
- Favorites / presets
- Lock screen shortcuts (Android 12+)
- Backup/restore settings
- Dark mode support

## Troubleshooting

### "Error: unreachable"
- Verify Mac is online and HTTP server is running
- Check firewall allows port 8888
- Verify IP address is correct (check Mac with `ifconfig`)
- Try **Retry All** from menu

### App crashes on launch
- Check Android Logcat: `adb logcat | grep volumecontrol`
- Ensure device has INTERNET permission (AndroidManifest.xml)

### Slider doesn't respond
- Check device error message (if shown)
- Device might be loading (see spinner icon)
- Try manual **Retry All**

### DataStore not persisting
- Check app has read/write permissions
- Try clearing app data: `adb shell pm clear com.volumecontrol.android`

## Contributing

See main `PROJECT_NOTES.md` in project root for development workflow.

## License

MIT - See LICENSE file in project root
