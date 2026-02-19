# macOS Menu Bar Volume Control App

A minimal macOS menu bar application that provides a SwiftUI-based volume control slider and HTTP API for system volume management.

## Features

- **Status Bar Icon**: Displays a speaker icon in the macOS menu bar
- **SwiftUI Popover**: Click the icon to open a volume slider control
- **System Volume Control**: Uses `osascript` to control system audio output and mute state
- **Mute Toggle**: Click the speaker icon to toggle mute on/off
- **HTTP API**: Listen on port 8888 for remote volume and mute control
  - `GET /` - Get current volume and mute status
  - `POST /` - Set volume and/or mute status (fields optional)
- **Auto-start on Login**: Automatically launches via LaunchAgent

## Prerequisites

Before building and installing, ensure you have:

1. **macOS Catalina 10.15 or later** (x86_64 architecture)
   - Target hardware: 2013 iMac or compatible
   - Note: SwiftUI is available starting macOS 10.15
   - Network framework available since macOS 10.14

2. **Xcode Command Line Tools**
   ```bash
   xcode-select --install
   ```
   This provides:
   - Swift compiler (`swiftc`)
   - Clang toolchain
   - macOS SDKs

3. **System Permissions**
   - On first run, you may be prompted to allow `osascript` Automation access
   - Grant access in **System Preferences → Security & Privacy → Automation**

## Build Instructions

### 1. Navigate to the Project

```bash
cd volume-control/macos
```

### 2. Build the Application

```bash
chmod +x build.sh
./build.sh
```

This script will:
- Compile Swift source files targeting macOS 10.15 x86_64
- Create a `.app` bundle at `VolumeControl.app`
- Remove quarantine attributes for easier launching

The build process uses:
- AppKit framework for menu bar integration
- Foundation framework for system calls
- Network framework for HTTP server
- SwiftUI for the popover interface

### 3. Install the Application

```bash
chmod +x install.sh
./install.sh
```

This script will:
- Copy `VolumeControl.app` to `/Applications/`
- Create a LaunchAgent plist file at `~/Library/LaunchAgents/com.volumecontrol.app.plist`
- Load the LaunchAgent to enable auto-start on login

The app will start immediately after installation and run in the background.

## Usage

### Menu Bar Icon

1. Look for the speaker icon (🔊) in the top-right menu bar
2. Click the icon to open the volume control popover
3. Drag the slider to adjust system volume (0-100%)
4. Click "Quit" to close the app

### HTTP API

The app listens on `http://localhost:8888` for API requests.

**Get current volume and mute status:**
```bash
curl http://localhost:8888/
```

Response:
```json
{"volume": 75, "muted": false}
```

**Set volume to specific level:**
```bash
curl -X POST http://localhost:8888/ \
     -H "Content-Type: application/json" \
     -d '{"volume": 50}'
```

Response:
```json
{"volume": 50, "muted": false}
```

**Toggle mute:**
```bash
curl -X POST http://localhost:8888/ \
     -H "Content-Type: application/json" \
     -d '{"muted": true}'
```

Response:
```json
{"volume": 75, "muted": true}
```

**Set both volume and mute:**
```bash
curl -X POST http://localhost:8888/ \
     -H "Content-Type: application/json" \
     -d '{"volume": 50, "muted": false}'
```

Response:
```json
{"volume": 50, "muted": false}
```

**Note**: When setting values, you can omit fields that you don't want to change. For example, `{"muted": true}` will toggle mute while keeping volume unchanged.

### Auto-Start on Login

The app automatically starts when you log in thanks to the LaunchAgent. To disable auto-start:

```bash
launchctl unload ~/Library/LaunchAgents/com.volumecontrol.app.plist
```

To re-enable:
```bash
launchctl load ~/Library/LaunchAgents/com.volumecontrol.app.plist
```

## File Structure

```
macos/
├── README.md                    # This file
├── Sources/
│   ├── main.swift              # App entry point
│   ├── AppDelegate.swift       # Status bar setup and popover management
│   ├── VolumeView.swift        # SwiftUI slider interface
│   ├── VolumeController.swift  # Volume control logic (osascript wrapper)
│   └── HTTPServer.swift        # HTTP server implementation
├── Info.plist                  # App configuration and metadata
├── build.sh                    # Build script (compiles and creates .app bundle)
└── install.sh                  # Install script (copies to /Applications and sets up LaunchAgent)
```

## Technical Details

### Architecture

- **AppDelegate**: Manages application lifecycle and status bar integration
- **VolumeController**: Observable object that bridges SwiftUI UI with system volume control
- **VolumeView**: SwiftUI view displaying the slider and quit button
- **HTTPServer**: Network framework listener handling HTTP requests on port 8888

### System Integration

- **osascript**: Commands used for volume control
  - Get: `osascript -e "output volume of (get volume settings)"`
  - Set: `osascript -e "set volume output volume <value>"`

- **LaunchAgent**: `com.volumecontrol.app.plist` enables auto-start
  - Stored in `~/Library/LaunchAgents/`
  - Runs at user login, not system-wide

- **Menu Bar**: Uses `NSStatusBar` (AppKit) for menu bar presence

## Troubleshooting

### App won't launch
- Ensure you've granted Automation permissions to `osascript`
  - System Preferences → Security & Privacy → Automation
- Try right-clicking the app and selecting "Open"

### Volume slider doesn't work
- Check that `osascript` has Automation permissions
- Verify your system volume isn't muted in macOS settings

### HTTP API not responding
- Ensure the app is running (icon visible in menu bar)
- Check port 8888 isn't in use: `lsof -i :8888`
- Try restarting the app

### Build fails
- Verify Xcode Command Line Tools are installed: `xcode-select --install`
- Check you're on a compatible macOS version (10.15+)
- Ensure you're building on x86_64 architecture (not Apple Silicon without cross-compilation)

### App doesn't auto-start
- Check LaunchAgent is loaded: `launchctl list | grep volumecontrol`
- Verify plist file exists: `cat ~/Library/LaunchAgents/com.volumecontrol.app.plist`

## Uninstall

To remove the app and auto-start:

```bash
# Disable auto-start
launchctl unload ~/Library/LaunchAgents/com.volumecontrol.app.plist

# Remove app
rm -rf /Applications/VolumeControl.app

# Remove LaunchAgent file
rm ~/Library/LaunchAgents/com.volumecontrol.app.plist
```

## Notes

- The app is unsigned and won't pass Gatekeeper verification. This is expected and handled via `xattr -cr` during build.
- The HTTP server has no authentication—only use on trusted local networks.
- Volume adjustments use the main audio output device.
- The app continues running after the popover closes; use the Quit button or Force Quit to exit.
