# Volume Control

A simple, open-source system for controlling volume across your devices - no complex setup, no proprietary services, just local network control.

## What is this?

Volume Control is a set of complementary apps that let you:

- **Control your Mac's volume** from your Android phone or Google TV
- **Control your Google TV's volume** from your Mac or Android phone
- **Use what you already have** - no special hardware, no subscriptions, just HTTP over WiFi

## Three Apps, One System

### 🍎 macOS App
Runs on your Mac as a menu bar app. Provides a simple slider to adjust system volume and opens a local HTTP server that accepts volume commands from other devices.

- Install once, auto-starts on login
- Click the 🔊 icon in your menu bar to adjust
- Works on older Macs too (supports Catalina 10.15+)

### 📱 Android Phone App
Control the volume of any macOS or Google TV device on your network.

- Add devices by IP address
- See all device volumes at a glance
- Drag sliders to adjust volumes in real-time
- Devices saved between app launches

### 📺 Google TV App
Runs automatically on your Google TV. Lets you control the TV's volume from anywhere on your network.

- Auto-starts when TV powers on
- Shows the TV's IP address on screen
- Responds to volume commands from Mac or Android phone
- Silent operation (minimal notifications)

## How It Works

Each app runs a small HTTP server on port 8888. They all speak the same language:

```
GET /volume      → What's the current volume?
POST /volume     → Set the volume to this level
```

That's it. No complicated setup. Everything happens over your local WiFi network.

## Quick Example

1. **Mac**: Install app → appears in menu bar
2. **Android phone**: Open app → add Mac's IP address
3. **Adjust**: Drag slider on phone → Mac volume changes instantly

Or from Google TV:
1. **TV**: App auto-starts and shows its IP
2. **Mac or Phone**: Add TV by IP address
3. **Control**: Adjust volume from anywhere

## Key Features

✓ **Local network only** - No internet needed, no cloud service
✓ **Auto-start** - Mac app loads on login, TV app loads on power-on
✓ **Persistent** - Device settings saved between app launches
✓ **Error handling** - Gracefully handles offline devices
✓ **No authentication** - Assumes your local network is trusted
✓ **Open source** - MIT licensed, see what it does

## Getting Started

See the README in each folder for installation and detailed usage:

- **macOS**: `macos/README.md`
- **Android**: `android/README.md`
- **Google TV**: `googletv/README.md`

## Architecture

```
macOS App ←→ HTTP (port 8888) ←→ Android Phone
                    ↓
            Google TV App
```

All three apps understand the same API. They're designed to work together, but each can also stand alone.

## Requirements

### For macOS
- macOS 10.15 (Catalina) or later
- Intel or Apple Silicon Mac

### For Android Phone
- Android 14 or later

### For Google TV
- Google TV running Android 12 (API 31) or later

### All
- WiFi network (all devices must be on the same network)
- No special setup - just install and add device IPs

## Common Uses

**Home theater**: Control TV volume from your couch using phone instead of remote
**Office**: Adjust presentation volume from your phone during a talk
**Kitchen**: Change music volume from your Mac while cooking
**Bedroom**: Control TV from bed without hunting for the remote

## License

MIT - Free to use, modify, and distribute. See LICENSE file.

## Contributing

This is a personal project, but issues and improvements are welcome. See PROJECT_NOTES.md in the root for development details.

---

**Questions?** Check the README in the specific app folder (`macos/`, `android/`, or `googletv/`) for troubleshooting and detailed instructions.
