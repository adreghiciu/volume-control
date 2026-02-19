# Volume Control Google TV App (Phase 3)

A Google TV app that runs an HTTP server to control TV volume from any device on the network.

## Features

- **HTTP API server**: Listens on port 8888 for volume and mute control requests
- **GET/POST endpoints**: `GET /` and `POST /` for full control
- **Mute control**: Get and set mute status via HTTP API
- **Auto-start on boot**: Runs as a foreground service that persists across reboots
- **Network detection**: Displays WiFi and Ethernet IP addresses in the UI
- **D-pad friendly UI**: Large 72sp text optimized for TV remote navigation
- **Silent notifications**: Foreground service runs without intrusive notifications
- **No external dependencies**: Pure Java HTTP server (no OkHttp, no Gson)

## Architecture

The app uses a **ForegroundService + HttpServer** architecture:

- **MainActivity.kt**: TV UI (Compose), displays status and IP addresses
- **VolumeService.kt**: Foreground service that manages the HTTP server lifecycle
- **HttpServer.kt**: Raw Java ServerSocket on port 8888, daemon threads, CRLF HTTP
- **VolumeController.kt**: AudioManager wrapper with 0-100 volume normalization
- **BootReceiver.kt**: Automatically starts the service on TV boot

## Tech Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| UI | Jetpack Compose + Material 3 | Modern, TV-friendly with large text |
| HTTP | Raw Java ServerSocket | No external dependencies, minimal overhead |
| Volume | AudioManager | Direct access to TV volume system |
| Service | ForegroundService + LocalBinder | Persistent, controllable from UI |
| Auto-start | BroadcastReceiver | Boots on TV power-on automatically |
| Concurrency | Kotlin Coroutines | Non-blocking I/O for HTTP |

## Building

### Prerequisites
- **Docker** (installed and configured)
- No need for Android Studio or SDK (Docker handles everything)

### Build Commands

```bash
cd googletv

# Build APK using Docker
docker build -t volume-control-googletv-builder .

# Extract APK from container to googletv directory
docker run --rm -v $(pwd):/output volume-control-googletv-builder \
  cp /workspace/VolumeControl-TV.apk /output/VolumeControl-TV.apk
```

The generated APK will be at: `googletv/VolumeControl-TV.apk` (22 MB debug build)

**Note**: First build takes ~2-3 minutes (downloads Docker layers). Subsequent builds are ~2 minutes (Docker caching).

## Installation

### Install on Google TV Device

1. Connect Google TV via USB to your Linux machine
2. Enable USB debugging on TV:
   - Settings → About → Tap "Build number" 7 times to unlock Developer Options
   - Settings → Advanced → Developer options → Toggle USB Debugging ON
3. Install the APK:
   ```bash
   adb install googletv/VolumeControl-TV.apk
   ```
4. App will appear in TV's app launcher as "Volume Control"

Alternatively, use network ADB:
```bash
adb connect <TV_IP>:5555  # First time may need to approve on TV screen
adb install googletv/VolumeControl-TV.apk
```

## Usage

### Launch the App
1. Find "Volume Control" in your TV's app launcher
2. Press OK/Select to open
3. The app displays:
   - Current TV volume (0-100)
   - WiFi IP address (if connected)
   - Ethernet IP address (if connected)
   - Service status (Running/Stopped)

### D-Pad Controls
- **Up**: Increase volume by 10%
- **Down**: Decrease volume by 10%
- **Left/Right**: Jump between UI elements
- **OK**: Toggle service on/off (button in UI)

### Service Management
- **App starts service automatically** on launch
- **Service persists** even if you close the app
- **Auto-restart on boot**: Service starts automatically when TV powers on
- **Tap "Stop Service"** button to manually stop

## API Integration

The app provides the same HTTP API as the macOS app:

- **GET `/`** → Returns `{"volume": 0-100, "muted": true|false}`
- **POST `/`** → Accepts optional `{"volume": <int>}` and/or `{"muted": <bool>}`

### Usage Examples

```bash
# From your Linux machine (if TV is at 192.168.68.106:8888)

# Get current volume and mute status
curl http://192.168.68.106:8888/
# Returns: {"volume": 75, "muted": false}

# Set volume to 50%
curl -X POST http://192.168.68.106:8888/ \
     -H "Content-Type: application/json" \
     -d '{"volume": 50}'
# Returns: {"volume": 50, "muted": false}

# Mute the TV
curl -X POST http://192.168.68.106:8888/ \
     -H "Content-Type: application/json" \
     -d '{"muted": true}'
# Returns: {"volume": 75, "muted": true}

# Set volume and mute together
curl -X POST http://192.168.68.106:8888/ \
     -H "Content-Type: application/json" \
     -d '{"volume": 40, "muted": false}'
# Returns: {"volume": 40, "muted": false}
```

**Note**: You can set just volume, just muted, or both. Omitted fields won't be changed.

### Control from Android Phone
Use the **Volume Control Android App** (Phase 2):
1. Add device with TV's IP and port 8888
2. Drag slider to adjust TV volume in real-time

### Control from macOS
Use the **HTTP API** directly with `curl` or any HTTP client

## Testing

### Setup
1. Install app on Google TV
2. Get TV's IP address (shown in app UI)
3. Ensure TV and control device are on same WiFi network

### Verify
```bash
# Get volume and mute status
curl http://<TV_IP>:8888/

# Set volume to 30%
curl -X POST http://<TV_IP>:8888/ \
     -H "Content-Type: application/json" \
     -d '{"volume": 30}'

# Mute the TV
curl -X POST http://<TV_IP>:8888/ \
     -H "Content-Type: application/json" \
     -d '{"muted": true}'

# Verify changes
curl http://<TV_IP>:8888/
```

### Manual Testing on TV
1. Open app
2. Use D-pad to adjust volume (Up = +10%, Down = -10%)
3. Press OK to toggle service on/off
4. Close app and reopen - service should still be running
5. Unplug TV power, plug back in - app should auto-start

## File Structure

```
googletv/
├── Dockerfile                          # Docker build configuration
├── build.gradle.kts                    # Root build config
├── gradle.properties                   # Gradle JVM options
├── settings.gradle.kts                 # Gradle settings
├── VolumeControl-TV.apk                # Generated APK (after build)
├── app/
│   ├── build.gradle.kts                # App-level gradle config (minSdk=31)
│   ├── proguard-rules.pro              # Obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml         # Permissions, receivers, service
│       └── java/com/volumecontrol/googletv/
│           ├── MainActivity.kt         # TV UI + ViewModel
│           ├── VolumeService.kt        # Foreground service
│           ├── HttpServer.kt           # Raw ServerSocket HTTP server
│           ├── VolumeController.kt     # AudioManager wrapper
│           └── BootReceiver.kt         # Auto-start on boot
```

## Permissions

The app requests these permissions (required for functionality):

- **INTERNET**: For HTTP server and network detection
- **FOREGROUND_SERVICE**: To run as persistent service
- **MODIFY_AUDIO_SETTINGS**: To control TV volume via AudioManager
- **POST_NOTIFICATIONS**: For foreground service notification
- **RECEIVE_BOOT_COMPLETED**: To auto-start on TV power-on
- **ACCESS_NETWORK_STATE**: To detect WiFi/Ethernet IP addresses

## API Implementation Details

### HTTP Server (HttpServer.kt)
- **Socket**: Raw Java ServerSocket on port 8888
- **Threads**: Daemon threads for each client connection
- **Protocol**: CRLF line endings (`\r\n`), no chunked encoding
- **JSON Parsing**: Regex-based (no external JSON library)
- **Response**: 200 OK for success, 400 for parse errors

### Volume Normalization (VolumeController.kt)
- **Raw API**: TV AudioManager uses arbitrary scale (0 to maxVolume)
- **Normalized**: App converts to 0-100 scale for consistency with macOS/Android apps
- **Formula**: `displayVolume = round(currentVolume / maxVolume * 100)`
- **Rounding**: Proper rounding ensures reversible conversions

## Known Limitations

1. **No authentication**: Assumes local network is trusted
2. **Android 31+**: Requires Android 12 (API 31) or later
3. **Volume only**: Only controls volume, not other TV settings
4. **Single HTTP server**: Only one app instance runs at a time
5. **Debug APK**: Not signed; only for testing (use `adb install`)

## Troubleshooting

### App won't install
- **Error**: "Requires newer sdk version #34"
  - TV is running Android 31 or older
  - Rebuild with `minSdk=31` in build.gradle.kts ✅ (already fixed)

### Can't connect via adb
- Verify USB debugging is enabled on TV
- Try network adb: `adb connect <TV_IP>:5555`
- If auth fails, look at TV screen - dialog may be waiting for approval

### HTTP server not responding
- Verify app is running (check foreground notification or app UI)
- Check TV firewall allows port 8888
- Verify TV IP address (shown in app)
- Try: `curl http://<TV_IP>:8888/volume`

### Volume not changing
- Verify POST request has correct JSON format: `{"volume": N}`
- Check TV isn't muted (may need to unmute manually)
- Try manual D-pad adjustment in app to verify AudioManager works

### Service keeps stopping
- Foreground service should persist - check notification settings
- Verify FOREGROUND_SERVICE permission granted
- Try rebooting TV if service repeatedly dies

### Auto-start not working
- Verify RECEIVE_BOOT_COMPLETED permission in AndroidManifest.xml
- Some TV manufacturers disable boot receivers - check settings
- Manual launch workaround: Add app icon to home screen

## Contributing

See main `PROJECT_NOTES.md` in project root for development workflow.

## License

MIT - See LICENSE file in project root
