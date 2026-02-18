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

### 🏗️ In Progress / Testing
- Testing macOS app on actual Catalina hardware (2013 iMac)
- Verifying HTTP API works correctly with curl

### 📋 Next Steps
1. Full testing of macOS app on target Mac
2. Start Android app development (Phase 2)
3. Add Google TV support (Phase 3)

## Architecture

### macOS App Structure
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

## Build & Install (On Mac)

```bash
cd macos
chmod +x build.sh install.sh
./build.sh      # Compiles to VolumeControl.app
./install.sh    # Copies to /Applications + sets up LaunchAgent
```

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

## Environment

- **Dev Machine**: Linux Mint (used Claude Code here)
- **Target Machine**: 2013 iMac with macOS Catalina 10.15 (x86_64)
- **VNC**: Remmina from Linux Mint → macOS (for testing)
- **SSH Keys**: Separate key per machine for security

## Future Considerations

- **Android Phase 2**: Use HTTP client to connect to port 8888
- **Google TV Phase 3**: Same HTTP API, different UI
- **Cross-platform testing**: Ensure HTTP API stability
- **Security**: Consider authentication for HTTP API if exposing beyond local network

## Useful Commands

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
- 2026-02-18
- Current status: macOS app built, HTTP API fixed, ready for testing
