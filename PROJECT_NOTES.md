# Volume Control Project - Development Notes

## Project Vision

A multi-platform volume control system:
1. **macOS app** (CURRENT - In Development)
   - Menu bar app with SwiftUI slider
   - HTTP API on port 8888
   - Auto-starts on login via LaunchAgent
   - Targets: macOS Catalina 10.15+ (x86_64, like 2013 iMac)

2. **Android Phone App** (PLANNED - Phase 2)
   - Remote control for Mac volume via HTTP API
   - Will communicate with macOS app on port 8888

3. **Google TV Android App** (PLANNED - Phase 3)
   - Control Google TV volume
   - Same HTTP API pattern

## Current Status

### ✅ Completed

#### Phase 1: macOS App (COMPLETE)
- macOS app core functionality implemented:
  - VolumeController: ObservableObject for volume get/set via osascript
  - VolumeView: SwiftUI popover with slider and quit button
  - AppDelegate: Status bar item (🔊 emoji) + popover management
  - HTTPServer: Network framework-based HTTP API on port 8888
  - Info.plist: App configuration with LSUIElement=YES (no Dock icon)
  - build.sh: Compilation script targeting x86_64-apple-macosx10.15
  - install.sh: Installation + LaunchAgent setup

- Fixes applied:
  - Catalina compatibility: SF Symbols → emoji icon (🔊)
  - HTTP response hanging: Fixed completion handler to wait for send before closing connection
  - Removed unused weak self capture (compiler warning)

#### Phase 2: Android App (COMPLETE)
- Android 14 app built with Kotlin + Jetpack Compose:
  - **Device Management**: Add/edit/delete devices with persistent storage via DataStore
  - **UI**: Material 3 design with one scrollable slider per device
  - **Volume Control**: Real-time slider updates that POST to macOS HTTP API
  - **Parallel Fetching**: All devices fetched concurrently on app focus (non-blocking)
  - **Error Handling**: Shows "unreachable" messages, disables sliders on error
  - **Retry Mechanism**: "Retry All" option in menu to re-fetch all volumes
  - **Architecture**: MVVM + StateFlow, clean separation of concerns
  - **API Client**: OkHttp with 3-second timeout (no auto-retry)
  - **Persistence**: DataStore + Gson for lightweight JSON storage

- File structure created:
  - Root gradle files (build.gradle.kts, settings.gradle.kts)
  - App gradle with all dependencies (Compose, OkHttp, DataStore, Gson)
  - 8 Kotlin source files (ViewModel, screens, API client, repository, models)
  - Resources (AndroidManifest.xml, themes, strings, icons)
  - Comprehensive README with usage instructions

### ✅ Android APK Build (COMPLETE)
- **Status**: APK successfully built using Docker
- **Approach**: Docker container with Android SDK + Gradle 8.10
- **Completed**:
  - ✅ Official Docker installed from Ubuntu repos
  - ✅ User added to docker group
  - ✅ Dockerfile created with Android SDK + Gradle 8.10
  - ✅ Fixed Java version compatibility (Java 21 for Kotlin 2.0.0)
  - ✅ Added Compose Compiler Gradle plugin for Kotlin 2.0
  - ✅ Fixed ExperimentalFoundationApi usage in DeviceCard.kt
  - ✅ Android APK built successfully: `VolumeControl.apk` (24 MB)
  - ✅ Cleaned up gradle artifacts (gradle-8.5, gradle zips, .gradle cache)
  - ✅ Documentation updated for Docker build process

**Output**:
- **APK Location**: `/home/adreghi/Work/volume-control/android/VolumeControl.apk`
- **Size**: 24 MB
- **Install**: `adb install android/VolumeControl.apk`
- **Config**: Debug build targeting Android 34 (Gradle 8.10, Kotlin 2.0.0)

### ✅ Android APK Testing & Debugging (COMPLETE)
- **Status**: Android app tested and fully working with macOS app
- **Testing Target**: Android 14 device, macOS Catalina server at 192.168.68.104:8888
- **Fixes Applied**:
  - ✅ **Cleartext HTTP**: Created `network_security_config.xml` allowing cleartext traffic for local network
  - ✅ **Threading**: Wrapped OkHttp calls with `Dispatchers.IO` to prevent NetworkOnMainThreadException
  - ✅ **Socket Timeouts**: Implemented 500ms debounce on slider to prevent excessive API calls
  - ✅ **State Management**: Modified setVolume to NOT update UI; only getVolume (fetched via LaunchedEffect) updates display
  - ✅ **Startup Display**: Added fetchAllVolumes() calls to loadDevices() and addDevice()
  - ✅ **Slider Jumps**: Fixed slider reverting to old values by implementing proper state management pattern
  - ✅ **UI Text**: Renamed "Retry All" → "Refresh" menu option
  - ✅ **Repository**: Cleaned and organized .gitignore files (root, android/, macos/)

**Key Pattern**:
- **Optimistic updates**: Slider updates UI immediately (pendingVolume) while request is in-flight
- **Server-driven display**: Volume display only updates from explicit getVolume calls, not from setVolume responses
- **Debounce behavior**: User sees pending value immediately, API request fires 500ms after user stops moving slider
- **Error recovery**: On setVolume error, calls fetchAllVolumes() to sync UI with actual device state

**Files Modified**:
- `android/app/src/main/res/xml/network_security_config.xml` (created)
- `android/app/src/main/AndroidManifest.xml` (added security config reference)
- `android/app/src/main/java/com/volumecontrol/android/data/VolumeApiClient.kt` (Dispatchers.IO, improved errors)
- `android/app/src/main/java/com/volumecontrol/android/ui/DeviceCard.kt` (debouncing, LaunchedEffect)
- `android/app/src/main/java/com/volumecontrol/android/ui/MainViewModel.kt` (fetchAllVolumes on load)
- `android/app/src/main/java/com/volumecontrol/android/MainActivity.kt` (removed duplicate call)
- `android/app/src/main/res/values/strings.xml` (renamed retry menu)
- `.gitignore`, `android/.gitignore`, `macos/.gitignore` (organized and cleaned)

### ✅ Phase 3: Google TV App (COMPLETE - Session 6)
- **Status**: Google TV app implemented as HTTP server on port 8888
- **Architecture**: ForegroundService managing HttpServer + VolumeController
- **Key Components**:
  - MainActivity.kt: TV UI (Compose) with 72sp text for D-pad navigation, dark theme (#1A1A2E)
  - VolumeService.kt: ForegroundService with START_STICKY auto-restart
  - HttpServer.kt: ServerSocket on port 8888 (no external libs), CRLF HTTP, regex JSON parsing
  - VolumeController.kt: AudioManager wrapper with 0-100 normalization using rounding
  - BootReceiver.kt: Auto-starts service on TV boot (ACTION_BOOT_COMPLETED + QUICKBOOT_POWERON)
- **HTTP API** (matches macOS/Android):
  - GET /volume → {"volume": N}
  - POST /volume -d '{"volume": N}' → {"volume": N}
- **Features**:
  - WiFi + Ethernet IP detection
  - Silent foreground notification (IMPORTANCE_LOW)
  - Leanback launcher intent for TV
  - Permissions: INTERNET, FOREGROUND_SERVICE, MODIFY_AUDIO_SETTINGS, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED
- **Build**: Docker build to APK (VolumeControl-TV.apk)
- **Files**: 20 new files (5 Kotlin sources, Gradle config, AndroidManifest, resources)

### 📋 Next Steps
1. ✅ Build Android APK using Docker (DONE - Session 4)
2. ✅ Test Android app on Android 14 device with macOS app running (DONE - Session 5)
3. ✅ Implement Google TV app (Phase 3) (DONE - Session 6)
4. ✅ Build Google TV APK using Docker (DONE - Session 7)
5. Test Google TV app on physical TV device

## Architecture

### macOS App Structure (Phase 1)
```
macos/
├── Sources/
│   ├── main.swift              # App entry point
│   ├── AppDelegate.swift       # Status bar + popover management
│   ├── VolumeView.swift        # SwiftUI slider UI
│   ├── VolumeController.swift  # Volume control logic (osascript wrapper)
│   └── HTTPServer.swift        # HTTP server (Network framework, port 8888)
├── Info.plist                  # App metadata
├── build.sh                    # Build script
├── install.sh                  # Install script
└── README.md                   # Build/install instructions
```

### Android App Structure (Phase 2)
```
android/
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Gradle settings
├── app/
│   ├── build.gradle.kts        # App dependencies (Compose, OkHttp, DataStore, Gson)
│   ├── proguard-rules.pro      # Obfuscation rules
│   ├── .gitignore
│   └── src/main/
│       ├── AndroidManifest.xml # App permissions, activities
│       └── java/com/volumecontrol/android/
│           ├── MainActivity.kt         # Entry point, onResume triggers fetch
│           ├── model/
│           │   └── Device.kt           # Device + DeviceState data classes
│           ├── data/
│           │   ├── DeviceRepository.kt # DataStore persistence
│           │   └── VolumeApiClient.kt  # OkHttp HTTP client (3s timeout)
│           ├── ui/
│           │   ├── MainViewModel.kt    # StateFlow, parallel fetching logic
│           │   ├── DeviceListScreen.kt # Main screen (TopAppBar, LazyColumn)
│           │   ├── DeviceCard.kt       # Device card with slider, error message
│           │   ├── AddEditDeviceDialog.kt # Add/edit device dialog
│           │   └── theme/
│           │       └── Theme.kt        # Material 3 colors, typography
│           └── res/
│               ├── values/             # strings, colors, themes
│               ├── drawable/           # icons (launcher background/foreground)
│               └── mipmap-*/           # adaptive icons
└── README.md                   # Usage, architecture, testing guide
```

### Google TV App Structure (Phase 3)
```
googletv/
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Gradle settings
├── gradle.properties           # Gradle settings
├── Dockerfile                  # Android SDK + Gradle 8.10 for building
├── .gitignore
├── app/
│   ├── build.gradle.kts        # App dependencies (Compose only, no OkHttp/DataStore/Gson)
│   ├── proguard-rules.pro      # Obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml # TV permissions, leanback feature
│       ├── java/com/volumecontrol/googletv/
│       │   ├── MainActivity.kt         # Activity + ViewModel + TvScreen UI (72sp text)
│       │   ├── VolumeService.kt        # ForegroundService, owns HttpServer lifecycle
│       │   ├── HttpServer.kt           # ServerSocket on port 8888, CRLF HTTP, regex JSON
│       │   ├── VolumeController.kt     # AudioManager wrapper, 0-100 normalization
│       │   └── BootReceiver.kt         # Auto-start on TV boot
│       └── res/
│           ├── values/                 # strings, colors, themes (dark #1A1A2E)
│           ├── drawable/               # icons (launcher background/foreground)
│           └── mipmap-*/               # adaptive icons
└── README.md                   # Build, install, API usage
```

## Key Technical Decisions

### Catalina Compatibility (10.15)
- **No @main attribute**: Not available until macOS 11, use NSApplication entry point instead
- **No MenuBarExtra**: Requires macOS 11+, use NSStatusBar (AppKit) instead
- **No SF Symbols**: Use emoji (🔊) for menu bar icon
- **Network framework**: Available in 10.14+, used for HTTP server
- **Compile target**: `-target x86_64-apple-macosx10.15`

### HTTP API Design
- **Port**: 8888
- **Endpoints**:
  - `GET /volume` → Returns `{"volume": 0-100}`
  - `POST /volume` → Accepts `{"volume": <int>}`, sets volume
- **No authentication**: Assumed local network is trusted
- **Manual HTTP parsing**: No external dependencies

### Volume Control
- Uses `osascript` (AppleScript via Process) for system volume control
- Requires user to grant Automation permission in System Preferences (first run)
- Commands:
  - Get: `osascript -e "output volume of (get volume settings)"`
  - Set: `osascript -e "set volume output volume <value>"`

### Auto-Start via LaunchAgent
- LaunchAgent plist at: `~/Library/LaunchAgents/com.volumecontrol.app.plist`
- No authentication required (user-level agent)
- Can be disabled with: `launchctl unload ~/Library/LaunchAgents/com.volumecontrol.app.plist`

## Build & Install

### macOS App (Phase 1)

```bash
cd macos
chmod +x build.sh install.sh
./build.sh      # Compiles to VolumeControl.app
./install.sh    # Copies to /Applications + sets up LaunchAgent
```

### Android App (Phase 2) - Docker Build & Install

**Quick Install** (preserves configured devices):
```bash
cd android
chmod +x reinstall.sh
./reinstall.sh  # Backs up devices → uninstalls old → installs new → restores devices
```

**Manual Build & Install**:
```bash
cd android

# Build APK (requires Docker)
docker build -t volume-control-android-builder .

# Extract APK from container to android directory
docker run --rm -v $(pwd):/output volume-control-android-builder \
  cp /workspace/VolumeControl.apk /output/VolumeControl.apk

# Install on Android device via USB
adb install VolumeControl.apk
```

**APK Output**: `android/VolumeControl.apk` (24 MB)

**Docker automatically handles**:
- Java 21 JDK
- Gradle 8.10
- Android SDK 34 + build tools
- Kotlin 2.0.0 + Compose Compiler
- APK naming and output

**Using reinstall.sh**:
- ✅ Automatically backs up DataStore (configured devices)
- ✅ Uninstalls old version
- ✅ Installs new APK
- ✅ Restores device configuration
- ✅ Saves backup to `/tmp/volume-control-backup-*` for 24h safety

## Testing

### Menu Bar Icon
- Look for 🔊 in menu bar (top-right)
- Click to open popover
- Slider should control system volume

### HTTP API
```bash
# Get volume
curl http://localhost:8888/volume
# Returns: {"volume": 75}

# Set volume to 50%
curl -X POST http://localhost:8888/volume \
     -H "Content-Type: application/json" \
     -d '{"volume": 50}'
# Returns: {"volume": 50}
```

## Known Issues & Notes

1. **osascript permissions**: On first run, user must grant Automation access
   - System Preferences → Security & Privacy → Automation
   - Approve both the app and osascript

2. **Unsigned app**: Won't pass Gatekeeper, but:
   - build.sh runs `xattr -cr` to remove quarantine
   - First launch needs right-click → Open, then it's in trust list

3. **VNC to Mac**: If testing remotely:
   - Enable Screen Sharing on Mac (System Preferences → Sharing)
   - Use Remmina (or other VNC client) from Linux
   - Clipboard sharing via VNC is unreliable, use Git/Gists instead

4. **Git workflow**:
   - Project on GitHub: git@github.com:adreghiciu/volume-control.git
   - License: MIT
   - Commits include co-authored-by: Claude Haiku 4.5
   - ⚠️ **CRITICAL Workflow**: DO NOT COMMIT/PUSH AUTOMATICALLY
     - After implementing features/fixes, make changes and build/test but DO NOT commit
     - Wait for explicit user instruction: "commit" or "push"
     - User may want to test functionality first before code is committed/pushed
     - This ensures quality testing and prevents premature/unwanted commits
     - Example workflow: Make code → Build/test → Wait for user → User says "commit" → Then commit & push

## Environment

- **Dev Machine**: Linux Mint (used Claude Code here)
- **Target Machine**: 2013 iMac with macOS Catalina 10.15 (x86_64)
- **VNC**: Remmina from Linux Mint → macOS (for testing)
- **SSH Keys**: Separate key per machine for security
- **Docker**: Installed and user added to docker group
  - ⚠️ **Important**: Always use `newgrp docker` before running docker commands
  - The user is in the docker group, but bash sessions don't recognize this until `newgrp` runs
  - Example: `newgrp docker << 'EOF' ... docker build ... EOF`
  - See "Useful Commands" section for Docker build examples

## Future Considerations

- **Android Phase 2**: Use HTTP client to connect to port 8888
- **Google TV Phase 3**: Same HTTP API, different UI
- **Cross-platform testing**: Ensure HTTP API stability
- **Security**: Consider authentication for HTTP API if exposing beyond local network

## Useful Commands

### Docker Commands (Android/Google TV APK builds)
```bash
# IMPORTANT: Always use newgrp docker to activate docker group permissions
# The user is in the docker group, but bash sessions don't recognize it until newgrp runs

# Build Android APK via Docker
newgrp docker << 'EOF'
cd android
docker build -t volume-control-android-builder .
docker run --rm -v $(pwd):/output volume-control-android-builder \
  cp /workspace/VolumeControl.apk /output/VolumeControl.apk
EOF

# Build Google TV APK via Docker
newgrp docker << 'EOF'
cd googletv
docker build -t volume-control-googletv-builder .
docker run --rm -v $(pwd):/output volume-control-googletv-builder \
  cp /workspace/VolumeControl-TV.apk /output/VolumeControl-TV.apk
EOF

# Quick docker test (after newgrp docker)
docker ps
docker images | grep volume-control
```

### macOS Commands
```bash
# Check if app is running
ps aux | grep VolumeControl

# Check LaunchAgent status
launchctl list | grep volumecontrol

# View LaunchAgent plist
cat ~/Library/LaunchAgents/com.volumecontrol.app.plist

# Check port 8888
lsof -i :8888

# Get Mac IP (for VNC)
ifconfig | grep "inet "

# Grant Automation permission to osascript (if needed)
# System Preferences → Security & Privacy → Automation
```

## Last Updated
- 2026-02-19 (Session 9 - Mute functionality fully implemented and working!)
- Current status:
  - Phase 1 (macOS): Mute feature complete, HTTP API on root / endpoint
  - Phase 2 (Android): Mute feature complete with speaker/muted icons, Mute All/Unmute All menu
  - Phase 3 (Google TV): Mute feature complete, all three apps fully functional

## Session History
- **Session 1**: Created macOS app, HTTP API, Android app code (all complete)
- **Session 2**: Setting up Docker on Linux Mint to build Android app
  - Discovered Homebrew Docker doesn't have systemd service
  - Chose Docker approach over Android Studio/SDK
  - Plan: Install official Docker, build APK in container
- **Session 3**: Docker setup completed
  - Installed official Docker from Ubuntu repos
  - Added user to docker group for non-sudo access
  - Created Dockerfile with Android SDK + build tools
- **Session 4**: Android APK build completed
  - Fixed Java 25 incompatibility: switched to Java 21
  - Upgraded Gradle 8.5 → 8.10 (required for AGP 8.5.0)
  - Added Kotlin Compose Compiler plugin for Kotlin 2.0.0
  - Fixed ExperimentalFoundationApi warnings
  - APK successfully built: 24 MB debug build
  - Ready for testing on Android 14 device
- **Session 5**: Android app tested and fully debugged
  - **Cleartext HTTP Issue**: Created network_security_config.xml to allow HTTP on local network (192.168.68.x)
  - **Threading Issue**: Added Dispatchers.IO wrapper to OkHttp calls (NetworkOnMainThreadException)
  - **Socket Timeout Issue**: Implemented 500ms slider debounce to prevent excessive API calls
  - **State Management**: Fixed setVolume pattern - only getVolume updates UI display, not setVolume responses
  - **Startup Display**: Added fetchAllVolumes() to loadDevices() so app shows correct volumes on launch
  - **Slider Behavior**: Implemented LaunchedEffect to update pendingVolume when actual volume is fetched
  - **Slider Jumping**: Fixed slider reverting to old values by separating optimistic UI update from server-driven state
  - **UI Polish**: Renamed "Retry All" → "Refresh" in menu
  - **Repository Cleanup**: Organized .gitignore files (root, android/, macos/) with only necessary ignores
  - **Verification**: App fully functional - can add devices, control volume, see real-time updates from separate curl commands
- **Session 6**: Google TV app implementation (Phase 3)
  - **Created**: 20 files for googletv/ module with complete HTTP server implementation
  - **Architecture**: ForegroundService + HttpServer + VolumeController pattern
  - **HttpServer.kt**: Pure Java ServerSocket (port 8888), daemon threads, CRLF HTTP responses, regex JSON parsing
  - **VolumeController.kt**: AudioManager wrapper with 0-100 normalization (rounding, @Synchronized)
  - **VolumeService.kt**: START_STICKY foreground service with LocalBinder, silent notification
  - **MainActivity.kt**: TV-optimized Compose UI (72sp text, dark theme #1A1A2E), IP detection, service status
  - **BootReceiver.kt**: Auto-start on TV boot (BOOT_COMPLETED + QUICKBOOT_POWERON)
  - **Permissions**: INTERNET, FOREGROUND_SERVICE, MODIFY_AUDIO_SETTINGS, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, ACCESS_NETWORK_STATE
  - **Features**: D-pad friendly UI, WiFi+Ethernet IP detection, silent foreground notification, leanback support
  - **Committed**: All 20 files with detailed commit message explaining architecture
  - **Build**: Pending Docker build after user group restart (user added to docker group)
- **Session 7**: Google TV APK build (Phase 3)
  - **Docker Build**: Successfully built with `newgrp docker` to activate group membership
  - **Import Fix**: Corrected `NetworkInterface` import from `android.net` → `java.net.NetworkInterface`
  - **APK Output**: VolumeControl-TV.apk (22 MB) successfully extracted to googletv directory
  - **Status**: Ready for testing on physical Google TV device via `adb install VolumeControl-TV.apk`
- **Session 8**: Android dark theme fixes (Phase 2 Polish)
  - **Theme Issue**: Splash screen and launcher icon were white/light, conflicting with dark Compose UI
  - **Fixed**: Updated themes.xml from `Theme.Material.Light.NoActionBar` → `Theme.Material.NoActionBar` (respects system dark mode)
  - **Launcher Icon**: Changed background #3DDC84 (green) → #0A0E1A (dark) to match dark theme
  - **Speaker Icon**: Changed foreground #000000 (black) → #6DB3FF (light blue) for visibility on dark background
  - **Rebuild**: APK rebuilt with theme changes using Docker
  - **Install**: Used reinstall.sh to preserve device configuration during update
  - **Documentation**: Updated PROJECT_NOTES.md with quick install workflow using reinstall.sh
- **Session 9**: Mute functionality implementation across all platforms (COMPLETE)
  - **API Redesign**: Changed all three apps from `/volume` endpoint to root `/` endpoint
  - **Response format**: All apps now return `{"volume": 0-100, "muted": true|false}`
  - **Request format**: POST accepts optional `volume` and/or `muted` fields (omitted fields unchanged)

  **macOS App**:
  - Added `@Published var muted` property to VolumeController
  - Added `getMutedStatus()` using osascript: `output muted of (get volume settings)`
  - Added `setMuted()` using osascript: `set volume output muted`
  - Fixed async/sync issue: moved state updates to synchronous path, dispatch UI notifications async
  - Updated HTTPServer to use root `/` endpoint returning both volume and muted
  - Updated install.sh with correct API examples

  **Google TV App**:
  - Added `isMuted()` and `setMuted()` methods using AudioManager.setStreamMute()
  - Updated HttpServer to use root `/` endpoint
  - Returns both volume and muted in all responses
  - Supports optional fields in POST requests

  **Android App**:
  - Created `MuteableVolumeSlider` Composable with clickable speaker/muted icon
  - Added `MutedIcon` (speaker without sound waves)
  - Updated DeviceCard to use MuteableVolumeSlider
  - Added `onMuteToggle` callback through DeviceListScreen to MainActivity to ViewModel
  - Updated MainViewModel with `toggleMute()`, `muteAll()`, `unmuteAll()` methods
  - Added "Mute All"/"Unmute All" menu items (context-aware based on state)
  - Updated VolumeApiClient with `getStatus()`, `setStatus()`, `setMuted()` methods
  - Created `VolumeStatus` data class holding both volume and muted
  - Fixed type system issues with ApiResult error handling
  - Updated strings.xml with mute menu items

  **Testing & Fixes**:
  - Verified all three apps working with correct state sync
  - Fixed macOS setMuted/setVolume async issue where HTTP response read old values
  - Tested mute toggle, individual device mute, mute all/unmute all
  - Android app successfully controls both macOS and Google TV devices

  **Documentation**:
  - Updated README.md for all three platforms (macOS, Android, Google TV)
  - Added mute feature documentation with API examples
  - Updated PROJECT_NOTES.md with implementation details
  - All READMEs now document the new root `/` endpoint and optional fields
